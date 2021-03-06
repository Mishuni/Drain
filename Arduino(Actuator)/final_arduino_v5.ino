#include <Servo.h>
#define PIN_SERIAL2_RX       (34ul)               // Pin description number for PIO_SERCOM on D12
#define PIN_SERIAL2_TX       (36ul)               // Pin description number for PIO_SERCOM on D10
#define PAD_SERIAL2_TX       (UART_TX_PAD_2)      // SERCOM pad 2
#define PAD_SERIAL2_RX       (SERCOM_RX_PAD_3)    // SERCOM pad 3
int analogPin = 0;    // 워터센서 analog port 0 연결 선언
int val = 0;          // 전류변화값 변수선언
int count = 0;
Servo myservo; 
int pos = 0; 
int A_1A = 6;
int A_1B = 5;
int angle = 110;
// Instantiate the Serial2 class
Uart Serial2(&sercom1, PIN_SERIAL2_RX, PIN_SERIAL2_TX, PAD_SERIAL2_RX, PAD_SERIAL2_TX);

void setup()
{
  Serial2.begin(9600);         // Begin Serial2
  Serial.begin(9600);
  pinMode(A_1A, OUTPUT);
  pinMode(A_1B, OUTPUT);
  myservo.attach(9);
  myservo.write(angle);
}

void loop()
{ 
  val = analogRead(analogPin);
  Serial.println(val);
  
  if( val> 400 && val < 830 ){
    //Serial2.write('N');
  }
  else if (val >= 830)        // Check if incoming data is available
  { if(count>=10){
        MotorOn();
        count = 0;
      }
      else{
        count++;
        //Serial2.write("W");
      }
  }
  else{
    //Serial2.write("N");
  }
  
  delay (2000);
  
} 
                      

void MotorOn()    // Interrupt handler for SERCOM1
{
      Serial.println("----motor on----");
      Serial2.write('S');
      pump(1);
      Serial.println(" Cleaner ! ");
      delay(1000);
      stop();
      delay(2000);
      Serial.println(" Moter ! ");
      for(int i = 10; i>=0; i--){
        delay(5); 
        for (pos = angle; pos <= 180; pos += 1) { // goes from 0 degrees to 180 degrees
        // in steps of 1 degree
            myservo.write(pos);              // tell servo to go to position in variable 'pos'
            delay(5);                       // waits 15ms for the servo to reach the position
        }
        for (pos = 180; pos >= angle; pos -= 1) { // goes from 180 degrees to 0 degrees
            myservo.write(pos);              // tell servo to go to position in variable 'pos'
            delay(5);                       // waits 15ms for the servo to reach the position
        }

        for (pos = angle; pos >= 0; pos -= 1) { // goes from 180 degrees to 0 degrees
            myservo.write(pos);              // tell servo to go to position in variable 'pos'
            delay(5);                       // waits 15ms for the servo to reach the position
        }
        for (pos = 0; pos <= angle; pos += 1) { // goes from 0 degrees to 180 degrees
        // in steps of 1 degree
            myservo.write(pos);              // tell servo to go to position in variable 'pos'
            delay(5);                       // waits 15ms for the servo to reach the position
        }
      }
      myservo.write(angle);
      Serial2.write("E");
      Serial.println("----motor off----");
    
}

void pump(int flag){
  boolean inPin1 = HIGH;
  boolean inPin2 = LOW;

  if(flag==1){
    inPin1 = HIGH;
    inPin2 = LOW;
  }

  digitalWrite(A_1A, inPin1);
  digitalWrite(A_1B, inPin2);
}

void stop(){
  digitalWrite(A_1A, LOW);
  digitalWrite(A_1B, LOW);
}

void SERCOM1_Handler()    // Interrupt handler for SERCOM1
{
  Serial2.IrqHandler();
}
