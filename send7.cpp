#include <stdio.h>
#include <string.h>
#include <errno.h>

#include <wiringSerial.h>

int main ()
{
  int fd ;

  if((fd=serialOpen("/dev/ttyUSB0",9600))<0){
    fprintf(stderr,"Unable to open serial device: %s\n",strerror(errno));
    return 1;
  }
  char c = 'c';
  serialPutchar (fd, c) ;
  while(true){
    if(serialDataAvail (fd))
    {
      char mes = serialGetchar(fd);
      printf (" -> %c\n", mes) ;  // 데이터 받는 함수 putchar
      if(mes=='E'){
        printf("Finish the cleaning\n");
      }
      else if(mes=='S'){
        printf("Starting the cleaning\n");
      }
      fflush (stdout) ;
    }
    }

}
