#include <Servo.h>
#define PIN_SERIAL2_RX       (34ul)               // Pin description number for PIO_SERCOM on D12
#define PIN_SERIAL2_TX       (36ul)               // Pin description number for PIO_SERCOM on D10
#define PAD_SERIAL2_TX       (UART_TX_PAD_2)      // SERCOM pad 2
#define PAD_SERIAL2_RX       (SERCOM_RX_PAD_3)    // SERCOM pad 3

Servo myservo; 
int pos = 0; 
// Instantiate the Serial2 class
Uart Serial2(&sercom1, PIN_SERIAL2_RX, PIN_SERIAL2_TX, PAD_SERIAL2_RX, PAD_SERIAL2_TX);

void setup()
{
  Serial2.begin(9600);         // Begin Serial2
  Serial.begin(9600);
  myservo.attach(9);
  
}

void loop()
{ 
  if (Serial2.available())        // Check if incoming data is available
  { 
    Serial2.write('S');
    char getData = Serial2.read();
    Serial.println(getData);
    Serial.println("motor on");
    
    for(int i = 5; i>=0; i--){
      for (pos = 0; pos <= 180; pos += 1) { // goes from 0 degrees to 180 degrees
      // in steps of 1 degree
          myservo.write(pos);              // tell servo to go to position in variable 'pos'
          delay(5);                       // waits 15ms for the servo to reach the position
      }
      for (pos = 180; pos >= 0; pos -= 1) { // goes from 180 degrees to 0 degrees
          myservo.write(pos);              // tell servo to go to position in variable 'pos'
          delay(5);                       // waits 15ms for the servo to reach the position
      }
    }
    Serial2.write('E');
   }
    
  
}   // Echo the byte back out on the serial port
                      


void SERCOM1_Handler()    // Interrupt handler for SERCOM1
{
  Serial2.IrqHandler();
}
