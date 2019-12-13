int add(int x, int y) {
	int z ;
	z = x+y;
	return z;
}

int max(int a, int b) {
	int c = 0;
	int d = 3;
	if (a > b) {
		c = a;
	} else {
		c = b;
	}
	-d;
	return d;
}

void whileFunc() {
	int x = 10;
	int count = 0;
	while (x < 15) {
		x = x + 1;
		count = count + 1;
	}
}

void main () {
	int t = 33;  
	int a = 10;
	int b = 0;
	_print(add(1,t));
	_print(max(1,2));
	if (!(t > 40)) {
		_print(t);
	}
	_print(a and b);
	_print(a or b);
	whileFunc();
}