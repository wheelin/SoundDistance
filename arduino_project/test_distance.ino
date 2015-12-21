#define echoPin 7
#define trigPin 8

long duration, distance;

int getDistance();

void setup()
{
	pinMode(trigPin, OUTPUT);
 	pinMode(echoPin, INPUT);
 	Serial.begin(115200);
}

void loop()
{
	Serial.print("Distance is :");
	Serial.println(getDistance());
	delay(1000);
}

int getDistance()
{
	digitalWrite(trigPin, LOW);
	delayMicroseconds(2);

	digitalWrite(trigPin, HIGH);
	delayMicroseconds(10);
	digitalWrite(trigPin, LOW);
	duration = pulseIn(echoPin, HIGH);
	return duration/58.2;
}