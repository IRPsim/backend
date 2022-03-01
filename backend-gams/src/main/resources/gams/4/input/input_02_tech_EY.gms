* - description: Please enter the conversion efficiency of the electrolyser
* - type: Float
* - identifier: conversion efficiency of the electrolyser
* - unit: [MW/ECU]
* - domain: (0,)
* - validation:
* - hidden:
* - processing:
SCALAR sca_S_eff conversion efficiency in power per ECU
$LOAD sca_S_eff

* - description: Please enter the maximal power of the electrolyzer
* - type: Float
* - identifier: maximal power of the electrolyzer
* - unit: [MW]
* - domain: (0,)
* - validation:
* - hidden:
* - processing:
SCALAR sca_S_max maximal power of electrolyzer
$LOAD sca_S_max

* - description: Please enter the minimal power of the electrolyzer
* - type: Float
* - identifier: minimal power of the electrolyzer
* - unit: [MW]
* - domain: (0,)
* - validation:
* - hidden:
* - processing:
SCALAR sca_S_min minimal power of the electrolyzer
$LOAD sca_S_min 

* - description: Please enter the maximal Ramp up of the electrolyzer
* - type: Float
* - identifier: Ramp up of the electrolyzer
* - unit: [MW/h]
* - domain: (0,)
* - validation:
* - hidden:
* - processing:
SCALAR sca_RU Ramp up of the electrolyzer
$LOAD sca_RU

* - description: Please enter the Ramp down of electrolyzer
* - type: Float
* - identifier: Ramp down of electrolyzer
* - unit: [MW/h]
* - domain: (0,)
* - validation:
* - hidden:
* - processing:
SCALAR sca_RD Ramp down of electrolyzer
$LOAD sca_RD
