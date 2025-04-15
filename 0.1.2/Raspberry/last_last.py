import RPi.GPIO as GPIO
import time
import cv2
import dlib
from scipy.spatial import distance as dist
from picamera2 import Picamera2
import os
import threading
import serial

# v_0.1.0 
import websocket
import json
import subprocess

# GPIO 설정
GPIO.setwarnings(False)  # 초기화 경고 억제
GPIO.setmode(GPIO.BCM)

# 부저, 릴레이, DC모터
BUZZER_PIN = 18
RELAY_PIN = 12
DC_MOTOR_PIN_1 = 17
DC_MOTOR_PIN_2 = 27
DC_MOTOR_PWM_PIN = 22

GPIO.setup(BUZZER_PIN, GPIO.OUT)
GPIO.setup(RELAY_PIN, GPIO.OUT)
GPIO.setup(DC_MOTOR_PIN_1, GPIO.OUT)
GPIO.setup(DC_MOTOR_PIN_2, GPIO.OUT)
GPIO.setup(DC_MOTOR_PWM_PIN, GPIO.OUT)

# PWM 설정
pwm = GPIO.PWM(BUZZER_PIN, 1000)
motor_pwm = GPIO.PWM(DC_MOTOR_PWM_PIN, 1000)
motor_pwm.start(0)

# EAR 및 타이머 설정
EYE_AR_THRESH = 0.25 # 0.25보다 값이 낮으면 졸음상태
ALERT_DURATION = 4 # 감지 지속시간
ALERT_INTERVAL = 5 # 경고 간격
close_start_time = None

# 0.1.1 감지 종료 변수 추가
sleep_detection_thread = None # Thread() 객체, start()를 통해 활성화 / 비활성화 플래그로 역할 수행
stop_sleep_detection = threading.Event() # is_set()이 참일 시 루프 자동종료

# 얼굴 감지 모델 초기화 (LOAD)
if not os.path.exists("shape_predictor_68_face_landmarks.dat"):
    raise FileNotFoundError("shape_predictor_68_face_landmarks.dat 파일이 현재 디렉토리에 없습니다.")

# 얼굴 감지 기능
hog_face_detector = dlib.get_frontal_face_detector()
# 눈 위치 추적 기능
dlib_facelandmark = dlib.shape_predictor("shape_predictor_68_face_landmarks.dat")

# 라즈베리파이 시리얼 넘버 가져오는 함수
def get_serial_number():
    try:
        serial = subprocess.check_output("cat /proc/cpuinfo | grep Serial | awk '{print $3}'", shell=True)
        return serial.decode('utf-8').strip()
    except Exception as e:
        print(f"시리얼 넘버 가져오기 실패: {e}")
        return "UNKNOWN"

# EAR 계산 함수 (눈 감김 여부 판단)
def calculate_EAR(eye):
    A = dist.euclidean(eye[1], eye[5])
    B = dist.euclidean(eye[2], eye[4])
    C = dist.euclidean(eye[0], eye[3])
    return (A + B) / (2.0 * C)

# 경고 함수 (부저 + 릴레이이)
def alert():
    pwm.start(50) # 부저 울림
    GPIO.output(RELAY_PIN, GPIO.HIGH) # 릴레이 ON 
    time.sleep(ALERT_INTERVAL) # 5초 후 OFF
    pwm.stop()
    GPIO.output(RELAY_PIN, GPIO.LOW)

# Picamera2 초기화
picam2 = Picamera2()
picam2.configure(picam2.create_preview_configuration({"size": (640, 480)}))
picam2.start()

# CO2 센서 설정
try:
    ser = serial.Serial(
        port='/dev/serial0',
        baudrate=9600,
        timeout=1
    )
except Exception as e:
    print(f"CO2 센서 초기화 오류: {e}")
    ser = None

# CO2 데이터 읽기 -- 모터 제어에서 사용
def read_co2():
    if ser:
        ser.write(b'\xff\x01\x86\x00\x00\x00\x00\x00\x79')
        time.sleep(0.1)
        response = ser.read(9)
        if len(response) == 9 and response[0] == 0xff and response[1] == 0x86:
            co2 = response[2] * 256 + response[3]
            # 농도반환
            return co2
    return None


# 모터 제어 함수 (thread1)
def motor_control():
    prev_co2 = None
    motor_active = False  # 모터가 작동 중인지 상태를 저장
    try:
        while True:
            co2 = read_co2()
            if co2 is not None:
                print(f"현재 CO2 농도: {co2} ppm")

                # CO2 2000PPM 이상일 때 모터 작동 
                if co2 >= 2000 and not motor_active:
                    print("CO2 농도 2000ppm 이상! 모터 작동.")
                    GPIO.output(DC_MOTOR_PIN_1, GPIO.HIGH)
                    GPIO.output(DC_MOTOR_PIN_2, GPIO.LOW)
                    motor_pwm.ChangeDutyCycle(100)
                    time.sleep(10)
                    motor_pwm.ChangeDutyCycle(0)
                    motor_active = True  # 모터가 이미 작동했음을 표시

                # 2000PPM 이하가 될 시 역방향 환기
                elif co2 < 2000 and motor_active:
                    print("CO2 농도 2000ppm 이하! 모터 반대 방향 작동.")
                    GPIO.output(DC_MOTOR_PIN_1, GPIO.LOW)
                    GPIO.output(DC_MOTOR_PIN_2, GPIO.HIGH)
                    motor_pwm.ChangeDutyCycle(100)
                    time.sleep(10)
                    motor_pwm.ChangeDutyCycle(0)
                    motor_active = False  # 모터가 반대로 작동 후 상태를 초기화

                else:
                    print("모터 작동 없음. CO2 농도 상태 유지.")
                    GPIO.output(DC_MOTOR_PIN_1, GPIO.LOW)
                    GPIO.output(DC_MOTOR_PIN_2, GPIO.LOW)
                    motor_pwm.ChangeDutyCycle(0)

                prev_co2 = co2
            else:
                print("CO2 데이터를 읽을 수 없습니다.")

            time.sleep(2)

    except KeyboardInterrupt:
        print("모터 제어 종료.")
    finally:
        GPIO.output(DC_MOTOR_PIN_1, GPIO.LOW)
        GPIO.output(DC_MOTOR_PIN_2, GPIO.LOW)
        motor_pwm.ChangeDutyCycle(0)

# EAR 계산함수 (졸음 감지에서 호출하여 사용)
def calibrate_ear():
    print("EAR 임계치 설정을 시작합니다. 눈을 크게 뜨거나 감고 데이터를 수집합니다.")
    ear_values = []

    try:
        while len(ear_values) < 300:
            frame = picam2.capture_array()
            gray = cv2.cvtColor(frame, cv2.COLOR_BGR2GRAY)

            faces = hog_face_detector(gray)
            for face in faces:
                face_landmarks = dlib_facelandmark(gray, face)
                leftEye = [(face_landmarks.part(n).x, face_landmarks.part(n).y) for n in range(36, 42)]
                rightEye = [(face_landmarks.part(n).x, face_landmarks.part(n).y) for n in range(42, 48)]
                left_ear = calculate_EAR(leftEye)
                right_ear = calculate_EAR(rightEye)
                EAR = (left_ear + right_ear) / 2
                ear_values.append(EAR)

                cv2.putText(frame, f"Collecting EAR: {len(ear_values)}/300", (10, 30), 
                            cv2.FONT_HERSHEY_SIMPLEX, 0.7, (0, 255, 0), 2)
                print(f"EAR: {EAR:.2f}")

            cv2.imshow("EAR Calibration", frame)

            if cv2.waitKey(1) & 0xFF == 27:  # ESC 키로 수집 중단 가능
                raise Exception("EAR 데이터 수집이 사용자가 중단되었습니다.")

        # EAR 임계값 계산
        open_values = sorted(ear_values)[-10:]  # 상위 10개 값 (눈을 크게 뜬 상태)
        close_values = sorted(ear_values)[:10]  # 하위 10개 값 (눈을 감은 상태)
        open_avg = sum(open_values) / len(open_values)
        close_avg = sum(close_values) / len(close_values)
        global EYE_AR_THRESH
        EYE_AR_THRESH = (open_avg + close_avg) / 2

        print("\nEAR threshold setup complete!")
        print(f"Average EAR with eyes open: {open_avg:.2f}")
        print(f"Average EAR with eyes closed: {close_avg:.2f}")
        print(f"Configured threshold (EYE_AR_THRESH): {EYE_AR_THRESH:.2f}")

    except Exception as e:
        print(f"EAR 임계치 설정 중 오류 발생: {e}")
        picam2.stop()
        cv2.destroyAllWindows()
        GPIO.cleanup()
        exit()

    finally:
        cv2.destroyAllWindows()


# 졸음 감지 함수 (thread2)
# EAR 값이 0.25미만으로 4초이상 지속되면 경고 발생
def detect_sleeping_driver():
    global close_start_time
    stop_sleep_detection.clear()  # 실행 전에 초기화

    try:
        while not stop_sleep_detection.is_set(): # 0.1.1 -- 감지종료 조건 추가
            frame = picam2.capture_array()
            gray = cv2.cvtColor(frame, cv2.COLOR_BGR2GRAY)
            faces = hog_face_detector(gray)

            for face in faces:
                face_landmarks = dlib_facelandmark(gray, face)
                leftEye = [(face_landmarks.part(n).x, face_landmarks.part(n).y) for n in range(36, 42)]
                rightEye = [(face_landmarks.part(n).x, face_landmarks.part(n).y) for n in range(42, 48)]
                left_ear = calculate_EAR(leftEye)
                right_ear = calculate_EAR(rightEye)
                EAR = (left_ear + right_ear) / 2

                if EAR < EYE_AR_THRESH:
                    if close_start_time is None:
                        close_start_time = time.time()
                    elif time.time() - close_start_time >= ALERT_DURATION:
                        print("졸음 감지! 경고!")
                        alert()
                else:
                    close_start_time = None

                print(f"EAR: {EAR:.2f}")

            if cv2.waitKey(1) & 0xFF == 27:
                break

    except KeyboardInterrupt:
        print("졸음 감지 종료.")
    finally:
        print("졸음 감지 루프 종료됨.")
        global sleep_detection_thread
        sleep_detection_thread = None
        close_start_time = None
        picam2.stop()
        cv2.destroyAllWindows()
        GPIO.cleanup()

# 0.1.0 WebSocket 수신 처리
def on_message(ws, message):
    try:
        data = json.loads(message)
        # 받은 명령이 start인 경우
        if data.get("command") == "start":
            if sleep_detection_thread is None or not sleep_detection_thread.is_alive():
                print("서버로부터 start 명령 수신 → 졸음 감지 시작")
                sleep_detection_thread = threading.Thread(target=detect_sleeping_driver)
                sleep_detection_thread.start() # 이 부분에서 sleep_dection_thread 객체를 스레드화 시킴 → 백그라운드에서 실행되는 플래그
            else:
                print("이미 감지 중입니다.")

        elif data.get("command") == "end":  
            print("서버로부터 end명령 수신 → 졸음 감지 중지")
            stop_sleep_detection.set()
        
    except Exception as e:
        print(f"WebSocket 메시지 처리 오류: {e}")

# 0.1.0 run_websocket 에서 호출, 서버에 시리얼 넘버 등록
def on_open(ws):
    serial_number = get_serial_number()
    print(f"WebSocket 연결됨 → 라즈베리파이 등록 중 (Serial: {serial_number})")
    ws.send(json.dumps({
        "type": "raspberry",
        "serial": serial_number
    }))

# 0.1.0 라즈베리파이 실행 시 자동실행, 지속적으로 실행되는 상태
def run_websocket():
    websocket.enableTrace(False)
    while True:
        try:
            ws = websocket.WebSocketApp(
                "ws://192.168.0.104:4141", 
                on_open=on_open,
                on_message=on_message,
            )
            ws.run_forever()
        # 연결 끊어질 시 5초 후 재연결 시도
        except Exception as e:
            print(f"WebSocket 연결 실패: {e}. 5초 후 재연결 시도...")
            time.sleep(5)


# 프로그램 실행부분
#  | | |
#  V V V

# EAR 임계치 설정 호출 : 최초 1회 호출
calibrate_ear()

# TODO : 모터 제어를 서버를 통해 비동기적으로 처리해야 함
# motor_thread = threading.Thread(target=motor_control) 
# motor_thread.start()

# 지속실행, 웹 소켓 연결 호출
websocket_thread = threading.Thread(target=run_websocket)
websocket_thread.start()

# motor_thread.join()
websocket_thread.join()