package chatServer;

import javafx.application.Application;

class t2{
	int a;
	public t2(int a) {
		this.a=a;
	}
	public void p() {
		a+=2;
		System.out.println(a);
	}
}

class t3{
	int a;
	public t3(int a) {
		this.a=a;
	}
	public void p() {
		a+=3;
		System.out.println(a);
	}
}

public class test {
	public static int a = 1;
	public static void main(String[] args) {
		System.out.println("kaishi"+a);
		t2 t2 = new t2(a);
		t3 t3 = new t3(a);
		t2.p();
		System.out.println("t2"+a);
		t3.p();
		t2.p();
		System.out.println("t3"+a);
	}
}

