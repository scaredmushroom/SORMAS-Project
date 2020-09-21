package de.symeda.sormas.backend.docgeneration;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.Set;

import javax.ejb.EJB;
import javax.ejb.Stateless;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.symeda.sormas.api.EntityDto;
import de.symeda.sormas.api.EntityDtoAccessHelper;
import de.symeda.sormas.api.EntityDtoAccessHelper.CachedReferenceDtoResolver;
import de.symeda.sormas.api.EntityDtoAccessHelper.IReferenceDtoResolver;
import de.symeda.sormas.api.ReferenceDto;
import de.symeda.sormas.api.caze.CaseDataDto;
import de.symeda.sormas.api.docgeneneration.QuarantineOrderFacade;
import de.symeda.sormas.api.facility.FacilityReferenceDto;
import de.symeda.sormas.api.infrastructure.PointOfEntryReferenceDto;
import de.symeda.sormas.api.person.PersonReferenceDto;
import de.symeda.sormas.api.region.CommunityReferenceDto;
import de.symeda.sormas.api.region.DistrictReferenceDto;
import de.symeda.sormas.api.region.RegionReferenceDto;
import de.symeda.sormas.api.user.UserReferenceDto;
import de.symeda.sormas.backend.caze.CaseFacadeEjb.CaseFacadeEjbLocal;
import de.symeda.sormas.backend.common.ConfigFacadeEjb.ConfigFacadeEjbLocal;
import de.symeda.sormas.backend.facility.FacilityFacadeEjb.FacilityFacadeEjbLocal;
import de.symeda.sormas.backend.infrastructure.PointOfEntryFacadeEjb.PointOfEntryFacadeEjbLocal;
import de.symeda.sormas.backend.person.PersonFacadeEjb.PersonFacadeEjbLocal;
import de.symeda.sormas.backend.region.CommunityFacadeEjb.CommunityFacadeEjbLocal;
import de.symeda.sormas.backend.region.DistrictFacadeEjb.DistrictFacadeEjbLocal;
import de.symeda.sormas.backend.region.RegionFacadeEjb.RegionFacadeEjbLocal;
import de.symeda.sormas.backend.user.UserFacadeEjb.UserFacadeEjbLocal;
import fr.opensagres.xdocreport.core.XDocReportException;

@Stateless(name = "QuarantineOrderFacade")
public class QuarantineOrderFacadeEjb implements QuarantineOrderFacade {

	private final Logger logger = LoggerFactory.getLogger(getClass());
	private static String DEFAULT_NULL_REPLACEMENT = "./.";

	@EJB
	private ConfigFacadeEjbLocal configFacade;

	@EJB
	private CaseFacadeEjbLocal caseFacade;

	@EJB
	private PersonFacadeEjbLocal personFacade;

	@EJB
	private UserFacadeEjbLocal userFacade;

	@EJB
	private RegionFacadeEjbLocal regionFacade;

	@EJB
	private DistrictFacadeEjbLocal districtFacade;

	@EJB
	private TemplateEngineService templateEngineService;

	@EJB
	private CommunityFacadeEjbLocal communityFacade;

	@EJB
	private FacilityFacadeEjbLocal facilityFacade;

	@EJB
	private PointOfEntryFacadeEjbLocal pointOfEntryFacade;

	@Override
	public byte[] getGeneratedDocument(String templateName, String caseUuid, Properties extraProperties) {
		// 1. Read template from custom directory
		String workflowTemplateDir = configFacade.getCustomFilesPath() + File.separator + "docgeneration" + File.separator + "quarantine";
		String templateFileName = workflowTemplateDir + File.separator + templateName;
		File templateFile = new File(templateFileName);

		if (!templateFile.exists()) {
			throw new IllegalArgumentException("Template file '" + templateName + "' not found.");
		}

		// 2. Extract document variables
		Set<String> propertyKeys;
		try {
			propertyKeys = templateEngineService.extractTemplateVariables(new FileInputStream(templateFile));
		} catch (IOException e) {
			throw new IllegalArgumentException("Could not read template file '" + templateName + "'.");
		} catch (XDocReportException e) {
			throw new IllegalArgumentException("Could not process template file '" + templateName + "'.");
		}

		Properties properties = new Properties();

		// 3. Map template variables to case data if possible
		// Naming conventions according sormas-api/src/main/resources/doc/SORMAS_Data_Dictionary.xlsx, e.g.:
		// Case.person.firstName
		// Case.quarantineFrom
		// Generic access as implemented in DataDictionaryGenerator.java
		// see also: DownloadUtil.createCsvExportStreamResource

		IReferenceDtoResolver referenceDtoResolver = getReferenceDtoResolver();

		CaseDataDto caseData = caseFacade.getCaseDataByUuid(caseUuid);
		if (caseData != null) {
			for (String propertyKey : propertyKeys) {
				if (propertyKey.startsWith("case.")) {
					String propertyPath = propertyKey.replace("case.", "");
					String propertyValue = EntityDtoAccessHelper.getPropertyPathValueString(caseData, propertyPath, referenceDtoResolver);
					System.out.println(propertyKey + ":" + propertyValue);
					properties.setProperty(propertyKey, propertyValue);
				}
			}
		}

		// 3. merge extra properties

		if (extraProperties != null) {
			for (String extraPropertyKey : extraProperties.stringPropertyNames()) {
				String propertyValue = extraProperties.getProperty(extraPropertyKey);
				System.out.println(extraPropertyKey + ":" + propertyValue);
				properties.setProperty(extraPropertyKey, propertyValue);
			}
		}

		// 4. fill null properties
		for (String propertyKey : propertyKeys) {
			if (properties.getProperty(propertyKey) == null || properties.getProperty(propertyKey).isEmpty()) {
				System.out.println(propertyKey + ":" + DEFAULT_NULL_REPLACEMENT);
				properties.setProperty(propertyKey, DEFAULT_NULL_REPLACEMENT);
			}
		}

		// 5. generate document

		try {
			return IOUtils.toByteArray(templateEngineService.generateDocument(properties, new FileInputStream(templateFile)));
		} catch (IOException | XDocReportException e) {
			logger.warn("Error while generating document", e);
			return null;
		}
	}

	private IReferenceDtoResolver getReferenceDtoResolver() {
		IReferenceDtoResolver referenceDtoResolver = new IReferenceDtoResolver() {

			@Override
			public EntityDto resolve(ReferenceDto referenceDto) {
				if (referenceDto == null) {
					return null;
				} else {
					String uuid = referenceDto.getUuid();
					Class<? extends ReferenceDto> referenceDtoClass = referenceDto.getClass();
					if (PersonReferenceDto.class.isAssignableFrom(referenceDtoClass)) {
						return personFacade.getPersonByUuid(uuid);
					} else if (UserReferenceDto.class.isAssignableFrom(referenceDtoClass)) {
						return userFacade.getByUuid(uuid);
					} else if (RegionReferenceDto.class.isAssignableFrom(referenceDtoClass)) {
						return regionFacade.getRegionByUuid(uuid);
					} else if (DistrictReferenceDto.class.isAssignableFrom(referenceDtoClass)) {
						return districtFacade.getDistrictByUuid(uuid);
					} else if (CommunityReferenceDto.class.isAssignableFrom(referenceDtoClass)) {
						return communityFacade.getByUuid(uuid);
					} else if (FacilityReferenceDto.class.isAssignableFrom(referenceDtoClass)) {
						return facilityFacade.getByUuid(uuid);
					} else if (PointOfEntryReferenceDto.class.isAssignableFrom(referenceDtoClass)) {
						return pointOfEntryFacade.getByUuid(uuid);
					}
					return null;
				}
			}
		};
		return new CachedReferenceDtoResolver(referenceDtoResolver);
	}
}