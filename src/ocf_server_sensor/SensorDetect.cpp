#include <iostream>
#include <errno.h>
#include <string.h>
#include <wiringPi.h>
#include <wiringSerial.h>

#define LED_R 12
#define LED_G 13
#define LED_B 14
using namespace std;


#include "SensorDetect.h"
#define LIGHT_SENSOR_IN 12


SensorDetect::SensorDetect(void)
{
	if(wiringPiSetup() < 0)
		cout << "Unable to setup wiringPi: " << strerror(errno) << endl;

	//pinMode(LIGHT_SENSOR_IN, INPUT);
	if((fd=serialOpen("/dev/ttyUSB0",9600))<0){
    fprintf(stderr,"Unable to open serial device: %s\n",strerror(errno));
	}
	if(wiringPiSetup() < 0)
		fprintf(stderr,"Unable to setup wiringPi: %s\n",strerror(errno));

	pinMode(LED_R, OUTPUT);
	pinMode(LED_G, OUTPUT);
	pinMode(LED_B, OUTPUT);

	digitalWrite(LED_R, HIGH);
	digitalWrite(LED_G, HIGH);
	digitalWrite(LED_B, HIGH);
}

int SensorDetect::Detect(void)
{
	if(serialDataAvail (fd))
    {
      char mes = serialGetchar(fd);
      printf (" -> %c\n", mes) ; 
      if(mes=='E'){
        printf("Finish the cleaning\n");
		fflush (stdout) ;
		res = 0;
		digitalWrite(LED_R, HIGH);
		digitalWrite(LED_G, HIGH);
		digitalWrite(LED_B, HIGH);
      }
      else if(mes=='S'){
        printf("Start the cleaning\n");
		fflush (stdout) ;
		res = 1;
		digitalWrite(LED_R, LOW);
		digitalWrite(LED_G, LOW);
		digitalWrite(LED_B, LOW);
      }
      else if(mes=='W'){
        printf("warning\n");
		fflush (stdout) ;
		res = 0;
		digitalWrite(LED_R, HIGH);
		digitalWrite(LED_G, HIGH);
		digitalWrite(LED_B, HIGH);
      }
	  else{
		  //other characters
		  res = 0;
		  if(mes=='N'){
			digitalWrite(LED_R, HIGH);
			digitalWrite(LED_G, HIGH);
			digitalWrite(LED_B, HIGH);
		  }
	  }   
    }

	return res;
	//return digitalRead(LIGHT_SENSOR_IN);
}

