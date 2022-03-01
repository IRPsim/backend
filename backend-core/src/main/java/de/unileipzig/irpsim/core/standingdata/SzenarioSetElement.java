package de.unileipzig.irpsim.core.standingdata;

import java.io.Serializable;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.ManyToOne;

import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@IdClass(value = SzenarioSetElement.SSE_Id.class)
public class SzenarioSetElement {

	public SzenarioSetElement() {

	}

	public SzenarioSetElement(final SzenarioSet set, final int stelle, final String name) {
		super();
		this.set = set;
		this.stelle = stelle;
		this.name = name;
	}

	static class SSE_Id implements Serializable {

      private static final long serialVersionUID = 1L;

      private int stelle;

		@ManyToOne
		private SzenarioSet set;

		public int getStelle() {
			return stelle;
		}

		public void setStelle(final int stelle) {
			this.stelle = stelle;
		}

		public SzenarioSet getSet() {
			return set;
		}

		public void setSet(final SzenarioSet set) {
			this.set = set;
		}

		@Override
		public boolean equals(final Object obj) {
			if (obj instanceof SSE_Id) {
				return ((SSE_Id) obj).stelle == stelle && ((SSE_Id) obj).set.equals(set);
			}
			return super.equals(obj);
		}

		@Override
		public int hashCode() {
			return set != null ? set.hashCode() + stelle : stelle;
		}

		@Override
		public String toString() {
			return stelle + " " + set.getJahr();
		}
	}

	@JsonIgnore
	@Id
	@ManyToOne
	private SzenarioSet set;

	@Id
	private int stelle;

	private String name;

	public int getStelle() {
		return stelle;
	}

	public void setStelle(final int stelle) {
		this.stelle = stelle;
	}

	public String getName() {
		return name;
	}

	public void setName(final String name) {
		this.name = name;
	}

	public SzenarioSet getSet() {
		return set;
	}

	public void setSet(final SzenarioSet set) {
		this.set = set;
	}

	@Override
	public String toString() {
		return "SSE " + stelle + " " + name;
	}
}
