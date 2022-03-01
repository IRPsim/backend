package de.unileipzig.irpsim.core.standingdata.data;

import java.util.HashMap;
import java.util.Map;

import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.CascadeType;

import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Entity
public class AlgebraicData extends Datensatz {

   private static final Logger LOG = LogManager.getLogger(AlgebraicData.class);
   
	/**
	 * Für das Frontend sollen Datensätze unabhängig vom Typ gleich aussehen. Deshalb existiert dieser Standard-View, um sicherzustellen, dass spezifische Eigenschaften algebraischer Daten nicht an
	 * das Frontend weitergegeben werden.
	 * 
	 * @author reichelt
	 *
	 */
	public static class JacksonRestrictedView {

	}

	@JsonView(JacksonRestrictedView.class)
	private String formel;

	private boolean evaluable = true;

	@JsonView(JacksonRestrictedView.class)
	@ElementCollection(fetch = FetchType.EAGER)
	@Cascade(CascadeType.ALL)
	private Map<String, Variable> variablenZuordnung = new HashMap<>();

	public boolean isEvaluable() {
		return evaluable;
	}

	public void setEvaluable(final boolean evaluable) {
		this.evaluable = evaluable;
	}

	public String getFormel() {
		return formel;
	}

	public void setFormel(final String formel) {
		this.formel = formel;
	}

	public Map<String, Variable> getVariablenZuordnung() {
		return variablenZuordnung;
	}

	public void setVariablenZuordnung(final Map<String, Variable> variablenZuordnung) {
		this.variablenZuordnung = variablenZuordnung;
	}

	public static void main(final String[] args) throws JsonProcessingException {
		final AlgebraicData example = new AlgebraicData();
		example.setSzenario(1);
		example.setJahr(2015);
		example.setFormel("A*5 + B * 100");

		final Map<String, Variable> zuordnung = new HashMap<>();
		{
			final Variable A = new Variable();
			A.setJahr(2015);
			final Stammdatum sdA = new Stammdatum();
			sdA.setId(15);
			A.setStammdatum(sdA);
			zuordnung.put("A", A);
		}

		{
			final Variable B = new Variable();
			B.setJahr(2015);
			final Stammdatum sdB = new Stammdatum();
			sdB.setId(27);
			B.setStammdatum(sdB);
			zuordnung.put("B", B);
		}

		example.setVariablenZuordnung(zuordnung);

		final String result = new ObjectMapper().writeValueAsString(example);
		LOG.debug(result);
	}
}