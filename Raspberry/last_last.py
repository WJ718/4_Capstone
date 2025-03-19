import RPi.GPIO as GPIO
import time
import cv2
import dlib
from scipy.spatial import distance as dist
from picamera2 import Picamera2
import os
import threading
import serial

# GPIO 설정
GPIO.setwarnings(False)  # 초기화 경고 억제
GPIO.setmode(GPIO.BCM)

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
EYE_AR_THRESH = 0.25
ALERT_DURATION = 4
ALERT_INTERVAL = 5
close_start_time = None

# 얼굴 감지 모델 초기화
if not os.path.exists("shape_predictor_68_face_landmarks.dat"):
    raise FileNotFoundError("shape_predictor_68_face_landmarks.dat 파일이 현재 디렉토리에 없습니다.")

hog_face_detector = dlib.get_frontal_face_detector()
dlib_facelandmark = dlib.shape_predictor("shape_predictor_68_face_landmarks.dat")

# EAR 계산 함수
def calculate_EAR(eye):
    A = dist.euclidean(eye[1], eye[5])
    B = dist.euclidean(eye[2], eye[4])
    C = dist.euclidean(eye[0], eye[3])
    return (A + B) / (2.0 * C)

# 경고 함수
def alert():
    pwm.start(50)
    GPIO.output(RELAY_PIN, GPIO.HIGH)
    time.sleep(ALERT_INTERVAL)
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

# CO2 데이터 읽기
def read_co2():
    if ser:
        ser.write(b'\xff\x01\x86\x00\x00\x00\x00\x00\x79')
        time.sleep(0.1)
        response = ser.read(9)
        if len(response) == 9 and response[0] == 0xff and response[1] == 0x86:
            co2 = response[2] * 256 + response[3]
            return co2
    return None

# 모터 제어
# 모터 제어
def motor_control():
    prev_co2 = None
    motor_active = False  # 모터가 작동 중인지 상태를 저장
    try:
        while True:
            co2 = read_co2()
            if co2 is not None:
                print(f"현재 CO2 농도: {co2} ppm")

                if co2 >= 2000 and not motor_active:
                    print("CO2 농도 2000ppm 이상! 모터 작동.")
                    GPIO.output(DC_MOTOR_PIN_1, GPIO.HIGH)
                    GPIO.output(DC_MOTOR_PIN_2, GPIO.LOW)
                    motor_pwm.ChangeDutyCycle(100)
                    time.sleep(10)
                    motor_pwm.ChangeDutyCycle(0)
                    motor_active = True  # 모터가 이미 작동했음을 표시

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


# 졸음 감지
def detect_sleeping_driver():
    global close_start_time
    try:
        while True:
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
        picam2.stop()
        cv2.destroyAllWindows()
        GPIO.cleanup()

# 스레드 실행
# EAR 임계치 설정 호출
calibrate_ear()

motor_thread = threading.Thread(target=motor_control)
sleep_detection_thread = threading.Thread(target=detect_sleeping_driver)

motor_thread.start()
sleep_detection_thread.start()

motor_thread.join()
sleep_detection_thread.join()
