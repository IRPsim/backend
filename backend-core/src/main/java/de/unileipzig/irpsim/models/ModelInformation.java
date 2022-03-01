package de.unileipzig.irpsim.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

public class ModelInformation {
   private int id;
   private String name;
   private String type;
   private String description;
   private String version;
   
   @JsonInclude(Include.NON_NULL)
   private int[] submodels;

   public ModelInformation() {

   }

   public ModelInformation(int id, String name, String type) {
      this.id = id;
      this.name = name;
      this.type = type;
   }

   public int getId() {
      return id;
   }

   public void setId(int id) {
      this.id = id;
   }

   public String getName() {
      return name;
   }

   public void setName(String name) {
      this.name = name;
   }

   public String getType() {
      return type;
   }

   public void setType(String type) {
      this.type = type;
   }

   public String getDescription() {
      return description;
   }

   public void setDescription(String description) {
      this.description = description;
   }

   public String getVersion() {
      return version;
   }

   public void setVersion(String version) {
      this.version = version;
   }

   public int[] getSubmodels() {
      return submodels;
   }

   public void setSubmodels(int[] submodels) {
      this.submodels = submodels;
   }
}
