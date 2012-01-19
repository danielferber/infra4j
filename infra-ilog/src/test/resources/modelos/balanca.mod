#-------------------------------------------------------------------------------
# Copyright 2012 Daniel Felix Ferber
# 
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
# 
#   http://www.apache.org/licenses/LICENSE-2.0
# 
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#-------------------------------------------------------------------------------
/*********************************************
 * OPL 12.2 Model
 * Author: X7WS
 * Creation Date: 10/06/2011 at 18:19:12
 *********************************************/
using CPLEX;

int P = ...;

dvar int+ p[1..P];
dvar int+ e[1..P][1..P];
dvar int+ d[1..P][1..P];
 
minimize sum(j in 1..P) p[j];

subject to {
	/* somente 0 ou 1 */
	forall (j in 1..P, i in 1..P) {
		e[i][j] <= 1;
		d[i][j] <= 1;
		e[i][j] <= 1 - d[i][j];
		d[i][j] <= 1 - e[i][j];
	}
}

subject to {
	forall (j in 1..P, i in 1..P) {
		p[j] >= e[i][j]+d[i][j];
	}
	forall (i in 1..P) {
		i + sum(j in 1..P) e[i][j]*j == sum(j in 1..P) d[i][j]*j;
	}
	forall (i in 1..P) {
	  	sum(j in 1..P) (e[i][j]+d[i][j]) <= sum(k in 1..P) p[k];
 	}	  
}
