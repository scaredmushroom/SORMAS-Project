package de.symeda.sormas.api.event;

import java.io.Serializable;

import de.symeda.sormas.api.person.ApproximateAgeType;
import de.symeda.sormas.api.person.ApproximateAgeType.ApproximateAgeHelper;
import de.symeda.sormas.api.person.Sex;
import de.symeda.sormas.api.utils.PersonalData;
import de.symeda.sormas.api.utils.SensitiveData;
import de.symeda.sormas.api.utils.jurisdiction.WithJurisdiction;

public class EventParticipantIndexDto implements WithJurisdiction<EventParticipantJurisdictionDto>, Serializable {

	private static final long serialVersionUID = 1136399297437006739L;

	public static final String I18N_PREFIX = "EventParticipant";

	public static final String UUID = "uuid";
	public static final String PERSON_UUID = "personUuid";
	public static final String CASE_UUID = "caseUuid";
	public static final String EVENT_UUID = "eventUuid";
	public static final String FIRST_NAME = "firstName";
	public static final String LAST_NAME = "lastName";
	public static final String SEX = "sex";
	public static final String APPROXIMATE_AGE = "approximateAge";
	public static final String INVOLVEMENT_DESCRIPTION = "involvementDescription";

	private String uuid;
	private String personUuid;
	private String caseUuid;
	private String eventUuid;
	@PersonalData
	@SensitiveData
	private String firstName;
	@SensitiveData
	private String lastName;
	private Sex sex;
	private String approximateAge;
	@SensitiveData
	private String involvementDescription;

	private EventParticipantJurisdictionDto eventJurisdiction;

	public EventParticipantIndexDto(
		String uuid,
		String personUuid,
		String caseUuid,
		String eventUuid,
		String firstName,
		String lastName,
		Sex sex,
		Integer approximateAge,
		ApproximateAgeType approximateAgeType,
		String involvementDescription,
		String reportingUserUuid) {

		this.uuid = uuid;
		this.personUuid = personUuid;
		this.caseUuid = caseUuid;
		this.eventUuid = eventUuid;
		this.firstName = firstName;
		this.lastName = lastName;
		this.sex = sex;
		this.approximateAge = ApproximateAgeHelper.formatApproximateAge(approximateAge, approximateAgeType);
		this.involvementDescription = involvementDescription;

		this.eventJurisdiction = new EventParticipantJurisdictionDto(reportingUserUuid);
	}

	public String getUuid() {
		return uuid;
	}

	public void setUuid(String uuid) {
		this.uuid = uuid;
	}

	public String getPersonUuid() {
		return personUuid;
	}

	public void setPersonUuid(String personUuid) {
		this.personUuid = personUuid;
	}

	public String getCaseUuid() {
		return caseUuid;
	}

	public void setCaseUuid(String caseUuid) {
		this.caseUuid = caseUuid;
	}

	public String getEventUuid() {
		return eventUuid;
	}

	public void setEventUuid(String eventUuid) {
		this.eventUuid = eventUuid;
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public Sex getSex() {
		return sex;
	}

	public void setSex(Sex sex) {
		this.sex = sex;
	}

	public String getApproximateAge() {
		return approximateAge;
	}

	public void setApproximateAge(String approximateAge) {
		this.approximateAge = approximateAge;
	}

	public String getInvolvementDescription() {
		return involvementDescription;
	}

	public void setInvolvementDescription(String involvementDescription) {
		this.involvementDescription = involvementDescription;
	}

	@Override
	public EventParticipantJurisdictionDto getJurisdiction() {
		return eventJurisdiction;
	}
}
