/*********************************************
 * OPL 12.2 Model
 * Author: X7WS
 * Creation Date: 21/06/2011 at 01:50:28
 *********************************************/

tuple ponto { 
	float x; 
	float y;
}

range espaco = 1..2;

ponto pontos[espaco] = [ <0,3>, <3,0> ];
ponto referencia = <1,1>;

dvar float pesos[espaco];

dexpr float x = sum(i in espaco) pesos[i] * pontos[i].x;
dexpr float y = sum(i in espaco) pesos[i] * pontos[i].y;

minimize (x - referencia.x)^2 + (y - referencia.y)^2;

subject to {
  sum(i in espaco) pesos[i] == 1.0;
  x >= 0;
  y >= 0;
}  

