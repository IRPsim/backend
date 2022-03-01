package de.unileipzig.irpsim.core.data.simulationparameters;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Daten eines Simulations-Parametersatzes.
 *
 * @author kluge
 */
@Entity
@Table
public class GdxConfiguration {
   private int id;
   private String name, creator;

   @JsonIgnore
   private String data;

   /**
    * Liefert die ID einer GDX-Konfiguration.
    *
    * @return Die ID der GDX-Konfiguration als Integer
    */
   @Id
   @GeneratedValue(strategy = GenerationType.IDENTITY)
   public int getId() {
      return id;
   }

   /**
    * Setzt die ID einer GDX-Konfiguration.
    *
    * @param id Die zu setzende ID
    */
   public void setId(final int id) {
      this.id = id;
   }

   /**
    * Liefert den Namen einer GDX-Konfiguration.
    *
    * @return Der Name der GDX-Konfiguration als String
    */
   public String getName() {
      return name;
   }

   /**
    * Setzt den Namen einer GDX-Konfiguration.
    *
    * @param name Der zu setzende Name
    */
   public void setName(final String name) {
      this.name = name;
   }

   /**
    * Liefert die Daten der GDX-Konfiguration.
    *
    * @return Die Daten der GDX-Konfiguration als String
    */
   @Column(columnDefinition = "LONGBLOB")
   @Basic(fetch = FetchType.LAZY)
   public String getData() {
      return data;
   }

   /**
    * Setzt die Daten der GDX-Konfiguration.
    *
    * @param data Die Daten der GDX-Konfiguration
    */
   public void setData(final String data) {
      this.data = data;
   }

   /**
    * Liefert den Namen des creators.
    *
    * @return Der Name des creators
    */
   public String getCreator() {
      return creator;
   }

   /**
    * Setzt den Namen des creators.
    *
    * @param creator Der zu setzende Name des creators
    */
   public void setCreator(final String creator) {
      this.creator = creator;
   }

   @Override
   public String toString() {
      return "id: " + this.id + " name: " + this.name + " creator: " + this.creator + " data: " + this.data;
   }
}
