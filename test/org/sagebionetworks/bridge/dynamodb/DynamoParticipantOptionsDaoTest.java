package org.sagebionetworks.bridge.dynamodb;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.sagebionetworks.bridge.TestConstants.TEST_STUDY_IDENTIFIER;
import static org.sagebionetworks.bridge.dao.ParticipantOption.*;

import java.util.Set;

import javax.annotation.Resource;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.sagebionetworks.bridge.BridgeUtils;
import org.sagebionetworks.bridge.dao.ParticipantOption.SharingScope;
import org.sagebionetworks.bridge.models.accounts.AllUserOptionsLookup;
import org.sagebionetworks.bridge.models.accounts.UserOptionsLookup;
import org.sagebionetworks.bridge.models.studies.Study;
import org.sagebionetworks.bridge.services.StudyService;

import com.google.common.collect.Sets;

import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@ContextConfiguration("classpath:test-context.xml")
@RunWith(SpringJUnit4ClassRunner.class)
public class DynamoParticipantOptionsDaoTest {

    private static final String TEST_EXT_ID = "AAA";
    private static final String TEST_EXT_ID_2 = "BBB";
    private static final String TEST_EXT_ID_3 = "CCC";
    
    private Study study;
    private String healthCode;
    
    @Resource
    DynamoParticipantOptionsDao optionsDao;
    
    @Resource
    StudyService studyService;

    @Before
    public void before() {
        study = studyService.getStudy(TEST_STUDY_IDENTIFIER);
        healthCode = BridgeUtils.generateGuid();
    }
    
    @After
    public void teardown() {
        optionsDao.deleteAllOptions(healthCode);
    }
    
    @Test
    public void crudOptions() {
        Set<String> dataGroups = Sets.newHashSet("group1", "group2", "group3");
        String sharingName = SharingScope.ALL_QUALIFIED_RESEARCHERS.name();
        
        // Set three options for an individual
        optionsDao.setOption(study, healthCode, SHARING_SCOPE, sharingName);
        optionsDao.setOption(study, healthCode, EXTERNAL_IDENTIFIER, TEST_EXT_ID);
        optionsDao.setOption(study,  healthCode, DATA_GROUPS, BridgeUtils.setToCommaList(dataGroups));

        // Verify all are set in the options map
        UserOptionsLookup lookup = optionsDao.getOptions(healthCode);
        assertEquals(SharingScope.ALL_QUALIFIED_RESEARCHERS, lookup.getEnum(SHARING_SCOPE, SharingScope.class));
        assertEquals(TEST_EXT_ID, lookup.getString(EXTERNAL_IDENTIFIER));
        assertEquals(dataGroups,  lookup.getStringSet(DATA_GROUPS));
        
        AllUserOptionsLookup allLookup = optionsDao.getOptionsForAllParticipants(study);
        
        // Verify all are set in the OptionLookup object (same option for all users)
        assertEquals(SharingScope.ALL_QUALIFIED_RESEARCHERS, allLookup.get(healthCode).getEnum(SHARING_SCOPE, SharingScope.class));
        assertEquals(TEST_EXT_ID, allLookup.get(healthCode).getString(EXTERNAL_IDENTIFIER));
        assertEquals(dataGroups, allLookup.get(healthCode).getStringSet(DATA_GROUPS));
        
        // Verify deleting one option
        optionsDao.deleteOption(healthCode, EXTERNAL_IDENTIFIER);
        assertNull(optionsDao.getOptions(healthCode).getString(EXTERNAL_IDENTIFIER));
        
        // Delete all options and verify they return to defaults
        optionsDao.deleteAllOptions(healthCode);

        allLookup = optionsDao.getOptionsForAllParticipants(study);
        
        assertEquals(SharingScope.NO_SHARING, allLookup.get(healthCode).getEnum(SHARING_SCOPE, SharingScope.class));
        assertNull(allLookup.get(healthCode).getString(EXTERNAL_IDENTIFIER));
        assertEquals(Sets.newHashSet(), allLookup.get(healthCode).getStringSet(DATA_GROUPS));
    }
    
    @Test
    public void getOptionLookupContainsCodesForAllUsers() {
        // Verify the lookup object contains records for multiple users
        optionsDao.setOption(study, healthCode, EXTERNAL_IDENTIFIER, TEST_EXT_ID);
        optionsDao.setOption(study, healthCode+"2", EXTERNAL_IDENTIFIER, TEST_EXT_ID_2);
        optionsDao.setOption(study, healthCode+"3", EXTERNAL_IDENTIFIER, TEST_EXT_ID_3);

        AllUserOptionsLookup allLookup = optionsDao.getOptionsForAllParticipants(study);
        
        assertEquals(TEST_EXT_ID, allLookup.get(healthCode).getString(EXTERNAL_IDENTIFIER));
        assertEquals(TEST_EXT_ID_2, allLookup.get(healthCode+"2").getString(EXTERNAL_IDENTIFIER));
        assertEquals(TEST_EXT_ID_3, allLookup.get(healthCode+"3").getString(EXTERNAL_IDENTIFIER));
        
        // healthCode's options are deleted in the @After method
        optionsDao.deleteAllOptions(healthCode+"2");
        optionsDao.deleteAllOptions(healthCode+"3");
    }
    
    @Test
    public void getAllOptionsForAllStudyParticipants() {
        optionsDao.setOption(study, healthCode, EXTERNAL_IDENTIFIER, TEST_EXT_ID);
        optionsDao.setOption(study, healthCode+"2", EXTERNAL_IDENTIFIER, TEST_EXT_ID_2);
        optionsDao.setOption(study, healthCode+"3", EXTERNAL_IDENTIFIER, TEST_EXT_ID_3);
        
        Set<String> dataGroups1 = Sets.newHashSet("group1");
        Set<String> dataGroups2 = Sets.newHashSet("group1","group2");
        Set<String> dataGroups3 = Sets.newHashSet("group1","group2","group3");
        optionsDao.setOption(study, healthCode, DATA_GROUPS, BridgeUtils.setToCommaList(dataGroups1));
        optionsDao.setOption(study, healthCode+"2", DATA_GROUPS, BridgeUtils.setToCommaList(dataGroups2));
        optionsDao.setOption(study, healthCode+"3", DATA_GROUPS, BridgeUtils.setToCommaList(dataGroups3));
        
        optionsDao.setOption(study, healthCode, SHARING_SCOPE, SharingScope.ALL_QUALIFIED_RESEARCHERS.name());
        optionsDao.setOption(study, healthCode+"2", SHARING_SCOPE, SharingScope.NO_SHARING.name());
        optionsDao.setOption(study, healthCode+"3", SHARING_SCOPE, SharingScope.SPONSORS_AND_PARTNERS.name());
        
        AllUserOptionsLookup allLookup = optionsDao.getOptionsForAllParticipants(study);
        
        assertEquals(TEST_EXT_ID, allLookup.get(healthCode).getString(EXTERNAL_IDENTIFIER));
        assertEquals(TEST_EXT_ID_2, allLookup.get(healthCode+"2").getString(EXTERNAL_IDENTIFIER));
        assertEquals(TEST_EXT_ID_3, allLookup.get(healthCode+"3").getString(EXTERNAL_IDENTIFIER));
        
        assertEquals(dataGroups1, allLookup.get(healthCode).getStringSet(DATA_GROUPS));
        assertEquals(dataGroups2, allLookup.get(healthCode+"2").getStringSet(DATA_GROUPS));
        assertEquals(dataGroups3, allLookup.get(healthCode+"3").getStringSet(DATA_GROUPS));
        
        assertEquals(SharingScope.ALL_QUALIFIED_RESEARCHERS, allLookup.get(healthCode).getEnum(SHARING_SCOPE, SharingScope.class));
        assertEquals(SharingScope.NO_SHARING, allLookup.get(healthCode+"2").getEnum(SHARING_SCOPE, SharingScope.class));
        assertEquals(SharingScope.SPONSORS_AND_PARTNERS, allLookup.get(healthCode+"3").getEnum(SHARING_SCOPE, SharingScope.class));
        
        // healthCode options are deleted in the @After method
        optionsDao.deleteAllOptions(healthCode+"2");
        optionsDao.deleteAllOptions(healthCode+"3");
    }

}
