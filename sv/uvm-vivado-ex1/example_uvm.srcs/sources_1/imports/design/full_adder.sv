module full_adder(x,y,cin,s,cout);
input x,y,cin;
output s,cout;
wire s1,c1,c2;
half_adder ha1(x,y,s1,c1);
half_adder ha2(cin,s1,s,c2);
or(cout,c1,c2);
endmodule
