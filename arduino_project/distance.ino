#include <SoftwareSerial.h>
#include <NewPing.h>

#define TRIGGER_PIN 12
#define ECHO_PIN 13
#define MAX_DIST 200

char incData[50];
int interpreteIncommingCmd(char * str);

NewPing sonar(TRIGGER_PIN, ECHO_PIN, MAX_DIST);
SoftwareSerial blt(10, 11);

char datas[20];

void setup()
{
    blt.begin(9600);
    Serial.begin(9600);
}

void loop()
{

    blt.println("Salut");
    if(blt.available())
    {
        char * str = blt.read();
        blt.print(str);
        Serial.prinln(str);
    }
    delay(100);
}

int interpreteIncommingCmd(char * str)
{
	String sstr = String(str);
	sstr.toLowerCase();
	sstr.trim();
}