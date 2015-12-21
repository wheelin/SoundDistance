#include <SoftwareSerial.h>

#define echoPin 7
#define trigPin 8
#define rxPin 10
#define txPin 11

double duration, distance;

double getDistance();
SoftwareSerial mySerial(rxPin, txPin); // RX, TX

String cmd;
char  dst_str[10];
char c;

void setup()
{
	pinMode(trigPin, OUTPUT);
 	pinMode(echoPin, INPUT);
 	Serial.begin(115200);
  while(!Serial);
  mySerial.begin(9600);
}

void loop()
{
  while(mySerial.available())
  {
    c = mySerial.read();
    if(c != '\n')
      cmd += c;
    else
    {
      Serial.println(cmd);
      cmd.trim();
      break;
    }
  }
  if(cmd.equals("meas"))
  {
    double dist = getDistance();
    sprintf(dst_str, " %04d\n", (int)(dist*10));
    Serial.print("Distance is :");
    Serial.println(dst_str);
    mySerial.print(dst_str);
    cmd = "";
  }
  else
  {
    cmd = "";
  }
	
	delay(100);
}

double getDistance()
{
	digitalWrite(trigPin, LOW);
	delayMicroseconds(2);
	digitalWrite(trigPin, HIGH);
	delayMicroseconds(10);
	digitalWrite(trigPin, LOW);
	duration = pulseIn(echoPin, HIGH);
	return duration/57;
}
