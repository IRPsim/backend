* - description: total cost over all timesteps
* - type: Float
* - identifier: total cost over all timesteps
* - unit: [EUR]
* - domain:
* - validation:
* - overview: 1
* - hidden:
PARAMETER total_cost;

* - description: Power of the electrolyzer at every timestep
* - type: Float
* - identifier: Power of the electrolyzer at every timestep
* - unit: [MW]
* - domain:
* - validation:
* - overview: 1
* - hidden:
PARAMETER power(set_ii);
