import RPi.GPIO as GPIO
import time
import cv2
import dlib
from scipy.spatial import distance as dist
import os
import threading
import serial
import websocket
import json
import subprocess

# GPIO 설정
GPIO.setwarnings(False)
GPIO.setmode(GPIO.BCM)

BUZZER_PIN = 18
RELAY_PIN = 12
DC_MOTOR_PIN_1 = 17
DC_MOTOR_PIN_2 = 27
DC_MOTOR_PWM_PIN = 22
enable_buzzer = True  # 기본값은 True (경고 시 소리 울림)

GPIO.setup(BUZZER_PIN, GPIO.OUT)
GPIO.setup(RELAY_PIN, GPIO.OUT)
GPIO.setup(DC_MOTOR_PIN_1, GPIO.OUT)
GPIO.setup(DC_MOTOR_PIN_2, GPIO.OUT)
GPIO.setup(DC_MOTOR_PWM_PIN, GPIO.OUT)

pwm = GPIO.PWM(BUZZER_PIN, 1000)
motor_pwm = GPIO.PWM(DC_MOTOR_PWM_PIN, 1000)
motor_pwm.start(0)

EYE_AR_THRESH = 0.25
ALERT_DURATION = 4
ALERT_INTERVAL = 5
close_start_time = None

sleep_detection_thread = None
stop_sleep_detection = threading.Event()
global ws_world

if not os.path.exists("shape_predictor_68_face_landmarks.dat"):
    raise FileNotFoundError("shape_predictor_68_face_landmarks.dat 파일이 없습니다.")

hog_face_detector = dlib.get_frontal_face_detector()
dlib_facelandmark = dlib.shape_predictor("shape_predictor_68_face_landmarks.dat")

def get_serial_number():
    try:
        serial = subprocess.check_output("cat /proc/cpuinfo | grep Serial | awk '{print $3}'", shell=True)
        return serial.decode('utf-8').strip()
    except Exception as e:
        print(f"시리얼 넘버 가져오기 실패: {e}")
        return "UNKNOWN"

def calculate_EAR(eye):
    A = dist.euclidean(eye[1], eye[5])
    B = dist.euclidean(eye[2], eye[4])
    C = dist.euclidean(eye[0], eye[3])
    return (A + B) / (2.0 * C)

def alert():
    if enable_buzzer:
        pwm.start(50)
    GPIO.output(RELAY_PIN, GPIO.HIGH)
    time.sleep(ALERT_INTERVAL)
    if enable_buzzer:
        pwm.stop()
    GPIO.output(RELAY_PIN, GPIO.LOW)


cap = cv2.VideoCapture(0)
if not cap.isOpened():
    raise RuntimeError("웹캠을 열 수 없습니다.")

try:
    ser = serial.Serial(port='/dev/serial0', baudrate=9600, timeout=1)
except Exception as e:
    print(f"CO2 센서 초기화 오류: {e}")
    ser = None

def read_co2():
    if ser:
        ser.write(b'\xff\x01\x86\x00\x00\x00\x00\x00\x79')
        time.sleep(0.1)
        response = ser.read(9)
        if len(response) == 9 and response[0] == 0xff and response[1] == 0x86:
            co2 = response[2] * 256 + response[3]
            return co2
    return None

def motor_control():
    motor_active = False
    try:
        while True:
            co2 = read_co2()
            if co2 is not None:
                print(f"[CO2 모니터링] 현재 CO2 농도: {co2} ppm")
                if co2 >= 2000 and not motor_active:
                    GPIO.output(DC_MOTOR_PIN_1, GPIO.HIGH)
                    GPIO.output(DC_MOTOR_PIN_2, GPIO.LOW)
                    motor_pwm.ChangeDutyCycle(100)
                    time.sleep(10)
                    motor_pwm.ChangeDutyCycle(0)
                    motor_active = True
                elif co2 < 2000 and motor_active:
                    GPIO.output(DC_MOTOR_PIN_1, GPIO.LOW)
                    GPIO.output(DC_MOTOR_PIN_2, GPIO.HIGH)
                    motor_pwm.ChangeDutyCycle(100)
                    time.sleep(10)
                    motor_pwm.ChangeDutyCycle(0)
                    motor_active = False
                else:
                    GPIO.output(DC_MOTOR_PIN_1, GPIO.LOW)
                    GPIO.output(DC_MOTOR_PIN_2, GPIO.LOW)
                    motor_pwm.ChangeDutyCycle(0)
            else:
                print("[CO2 모니터링] 데이터를 읽을 수 없습니다.")
            time.sleep(2)
    except KeyboardInterrupt:
        pass
    finally:
        GPIO.output(DC_MOTOR_PIN_1, GPIO.LOW)
        GPIO.output(DC_MOTOR_PIN_2, GPIO.LOW)
        motor_pwm.ChangeDutyCycle(0)

def calibrate_ear():
    print("[시작] EAR 임계치 설정 중...")
    ear_values = []
    try:
        while len(ear_values) < 300:
            ret, frame = cap.read()
            if not ret:
                continue
            gray = cv2.cvtColor(frame, cv2.COLOR_BGR2GRAY)
            faces = hog_face_detector(gray)
            for face in faces:
                landmarks = dlib_facelandmark(gray, face)
                leftEye = [(landmarks.part(n).x, landmarks.part(n).y) for n in range(36, 42)]
                rightEye = [(landmarks.part(n).x, landmarks.part(n).y) for n in range(42, 48)]
                EAR = (calculate_EAR(leftEye) + calculate_EAR(rightEye)) / 2
                ear_values.append(EAR)
                cv2.putText(frame, f"Collecting EAR: {len(ear_values)}/300", (10, 30),
                            cv2.FONT_HERSHEY_SIMPLEX, 0.7, (0, 255, 0), 2)
                print(f"[EAR 캘리브레이션] EAR: {EAR:.2f}")
            cv2.imshow("EAR Calibration", frame)
            if cv2.waitKey(1) & 0xFF == 27:
                raise Exception("중단됨")
        open_avg = sum(sorted(ear_values)[-10:]) / 10
        close_avg = sum(sorted(ear_values)[:10]) / 10
        global EYE_AR_THRESH
        EYE_AR_THRESH = (open_avg + close_avg) / 2
        print(f"[완료] EAR 기준값 설정됨: {EYE_AR_THRESH:.2f}")
    except Exception as e:
        print(e)
        cap.release()
        cv2.destroyAllWindows()
        GPIO.cleanup()
        exit()
    finally:
        cv2.destroyAllWindows()

def detect_sleeping_driver():
    print("[시작] 졸음 감지 모니터링 중...")
    global close_start_time
    try:
        while not stop_sleep_detection.is_set():
            ret, frame = cap.read()
            if not ret:
                continue
            gray = cv2.cvtColor(frame, cv2.COLOR_BGR2GRAY)
            faces = hog_face_detector(gray)
            for face in faces:
                landmarks = dlib_facelandmark(gray, face)
                leftEye = [(landmarks.part(n).x, landmarks.part(n).y) for n in range(36, 42)]
                rightEye = [(landmarks.part(n).x, landmarks.part(n).y) for n in range(42, 48)]
                EAR = (calculate_EAR(leftEye) + calculate_EAR(rightEye)) / 2
                if EAR < EYE_AR_THRESH:
                    if close_start_time is None:
                        close_start_time = time.time()
                    elif time.time() - close_start_time >= ALERT_DURATION:
                        print("[경고] 졸음 감지! 경고 발생")
                        alert()
                        send_sleepingalert("졸음이 감지되었습니다")
                else:
                    close_start_time = None
                print(f"[EAR 모니터링] EAR: {EAR:.2f}")
            if cv2.waitKey(1) & 0xFF == 27:
                break
    except KeyboardInterrupt:
        pass
    finally:
        global sleep_detection_thread
        sleep_detection_thread = None
        close_start_time = None
        cap.release()
        cv2.destroyAllWindows()
        GPIO.cleanup()

def send_sleepingalert(alert_sign):
    if not ws_world:
        print("[WebSocket] 연결 없음 - 알림 전송 불가")
    else:
        try:
            material = {
                'type': 'sleepy',
                'message': alert_sign,
                'timeline': time.time(),
                'serial_number': get_serial_number()
            }
            ws_world.send(json.dumps(material))
            print("[WebSocket] 졸음 알림 전송 완료")
        except Exception as e:
            print(f"[WebSocket 오류] 전송 실패: {e}")

# WebSocket 수신 처리
def on_message(ws, message):
    try:
        data = json.loads(message)
        
        # start 명령 처리
        if data.get("command") == "start":
            if sleep_detection_thread is None or not sleep_detection_thread.is_alive():
                print("[WebSocket] 서버로부터 start 명령 수신 → 졸음 감지 시작")
                calibrate_ear()
                stop_sleep_detection.clear() # stop 했을 때 set했던 것 초기화
                sleep_detection_thread = threading.Thread(target=detect_sleeping_driver)
                sleep_detection_thread.start() # 이 부분에서 sleep_dection_thread 객체를 스레드화 시킴 → 백그라운드에서 실행되는 플래그
            else:
                print("이미 감지 중입니다.") 

        # end 명령 처리
        elif data.get("command")  == "end":
            print('[WebSocket] 서버로부터 end 명령 수신 --> 졸음 감지 중지')
            stop_sleep_detection.set()

        # set-sound 처리
        elif data.get("command") == "set-sound":
            value = data.get("value", "true").lower()
            global enable_buzzer
            enable_buzzer = (value == "true")
            print(f"[WebSocket] 서버로부터 사운드 설정 수신 → 값: {enable_buzzer}")


        # set-led 처리
        elif data.get("command") == "set-led":
            """
            LED 값 처리 로직 (보류)

            """
            print(f"[WebSocket] 서버로부터 밝기 설정 수신 → 값: {data.get('value')}")
        
    except Exception as e:
        print(f"WebSocket 메시지 처리 오류: {e}")

def on_open(ws):
    global ws_world
    ws_world = ws
    serial_number = get_serial_number()
    print(f"[WebSocket] 연결됨 → Serial: {serial_number}")
    ws.send(json.dumps({"type": "raspberry", "serial": serial_number}))

def run_websocket():
    websocket.enableTrace(False)
    while True:
        try:
            print("[WebSocket] 서버 연결 대기 중...")
            ws = websocket.WebSocketApp(
                "ws://192.168.0.4:4141",
                on_open=on_open,
                on_message=on_message
            )
            ws.run_forever()
        except Exception as e:
            print(f"[WebSocket] 재연결 시도 중 오류: {e}")
            global ws_world
            ws_world = None
            time.sleep(5)

# 메인 실행부
websocket_thread = threading.Thread(target=run_websocket)
websocket_thread.start()

# CO2 모니터링 스레드 추가 실행
#motor_thread = threading.Thread(target=motor_control)
#motor_thread.start()

websocket_thread.join()
#motor_thread.join()
