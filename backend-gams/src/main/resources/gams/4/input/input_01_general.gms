* - description: Einlesen des Simulationshorizonts
* - type: TimeSeries
* - identifier: Simulationshorizont
* - unit: [Zeitschritt pro h]
* - hidden: 1
SET set_ii_0(*) Simulationshorizont inkl 0
$LOAD set_ii_0

* - description: Einlesen des Simulationshorizonts
* - type: TimeSeries
* - identifier: Simulationshorizont
* - unit: [Zeitschritt pro h]
* - hidden: 1
SET set_ii(set_ii_0) Simulationshorizont
$LOAD set_ii

* - description: Einlesen des Optimierungshorizonts
* - type: TimeSeries
* - identifier: Optimierungshorizont
* - hidden: 1
SET set_t(set_ii) Optimierungshorizont
$LOAD set_t

* - description: day ahead spot price
* - type: Float
* - identifier: day ahead spot price
* - unit: [EUR/MWh]
* - domain: [0,)
* - default: 0
* - validation:
* - hidden:
* - processing:
PARAMETER par_prc_el(set_ii) day ahead spot price;
$LOAD prc_el

* - description: Please enter the demand for chlorine
* - type: Float
* - identifier: demand for chlorine
* - unit: [tCl]
* - domain: [0,)
* - default: 0
* - validation:
* - hidden:
* - processing:
PARAMETER par_D_Cl(set_ii) demand for chlorine;
$LOAD D_Cl

* - description: Please enter the total demand for chlorine 
* - type: Float
* - identifier: demand for chlorine for whole period 
* - unit: [tCl]
* - domain: [0,)
* - default: 0
* - validation:
* - hidden:
* - processing:
SCALAR D_Cl_tot demand for chlorine for whole period in tCl
$LOAD par_D_Cl_tot
