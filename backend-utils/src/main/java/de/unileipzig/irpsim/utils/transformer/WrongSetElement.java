package de.unileipzig.irpsim.utils.transformer;

class WrongSetElement {
   String parameterName;
   String setElement;
   String setName;

   public WrongSetElement(final String parameterName, final String setElement, final String setName) {
      super();
      this.parameterName = parameterName;
      this.setElement = setElement;
      this.setName = setName;
   }

   @Override
   public String toString() {
      return "Parameter: " + parameterName + " Set-Element: " + setElement + " Setname: " + setName;
   }

}