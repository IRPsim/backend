package de.unileipzig.irpsim.core.standingdata;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table
public class SzenarioSet {

	@Id
	private int jahr;

	@OneToMany(mappedBy = "set")
	private List<SzenarioSetElement> szenarien = new LinkedList<>();

	public int getJahr() {
		return jahr;
	}

	public void setJahr(final int jahr) {
		this.jahr = jahr;
	}

	public List<SzenarioSetElement> getSzenarien() {
		return szenarien;
	}

	public void setSzenarien(final List<SzenarioSetElement> szenarien) {
		this.szenarien = szenarien;
	}

	@JsonIgnore
	public List<Integer> getIds() {
		return szenarien.stream().mapToInt(szenario -> szenario.getStelle()).boxed().collect(Collectors.toList());
	}

	public boolean hasStelle(final int stelle) {
		for (final SzenarioSetElement element : szenarien) {
			if (element.getStelle() == stelle) {
				return true;
			}
		}
		return false;
	}
}
