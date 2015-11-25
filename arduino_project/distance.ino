#include <stdint.h>

uint8_t interpreteCommand(String str);
uint8_t configureBluetooth(void);

String cmd;

void setup() {
  // put your setup code here, to run once:
  Serial.begin(115200);
  Serial5.begin(9600);
  while(!Serial);
  while(!Serial5);
}

void loop() {
  uint8_t cmdType = 0;
  int val = random(0, 4000);
  if(Serial5.available() > 0)
  {
    cmd = Serial5.readString();
    cmdType = interpreteCommand(cmd);
  }
  else 
  {
    Serial.println("none");
  }
  switch(cmdType)
  {
	case 1:
	  Serial.println("INV_CMD");
	  Serial5.println("#INV_CMD");
	  break;
	case 2:
	  Serial.println("MEAS");
	  Serial5.println("#MEAS");
	  Serial5.println(val);
	  break;
	default:
	  break;
  }
  delay(500);
}

uint8_t interpreteCommand(String str)
{
  str.trim();
  str.toUpperCase();
  if(str.compareTo("MEAS") == 0) return 2;
  else return 1;
}

uint8_t configureBluetooth(void)
{
  
}
