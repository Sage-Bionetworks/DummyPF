package org.sagebionetworks.bridge.play.controllers;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import org.sagebionetworks.bridge.Roles;
import org.sagebionetworks.bridge.TestConstants;
import org.sagebionetworks.bridge.TestUtils;
import org.sagebionetworks.bridge.dynamodb.DynamoStudy;
import org.sagebionetworks.bridge.json.BridgeObjectMapper;
import org.sagebionetworks.bridge.models.PagedResourceList;
import org.sagebionetworks.bridge.models.accounts.ExternalIdentifierInfo;
import org.sagebionetworks.bridge.models.accounts.User;
import org.sagebionetworks.bridge.models.accounts.UserSession;
import org.sagebionetworks.bridge.models.studies.Study;
import org.sagebionetworks.bridge.services.ExternalIdService;
import org.sagebionetworks.bridge.services.StudyService;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Lists;

import play.mvc.Result;
import play.test.Helpers;

@RunWith(MockitoJUnitRunner.class)
public class ExternalIdControllerTest {
    
    private static final BridgeObjectMapper MAPPER = BridgeObjectMapper.get();
    
    private static final List<ExternalIdentifierInfo> EXT_IDS = Lists.newArrayList(
            new ExternalIdentifierInfo("AAA", false), new ExternalIdentifierInfo("BBB", false),
            new ExternalIdentifierInfo("CCC", false));
    
    private static final TypeReference<PagedResourceList<ExternalIdentifierInfo>> PAGE_REF = 
            new TypeReference<PagedResourceList<ExternalIdentifierInfo>>() {};
    
    @Mock
    ExternalIdService externalIdService;
    
    @Mock
    StudyService studyService;
    
    Study study;
    
    ExternalIdController controller;
    
    @Before
    public void before() throws Exception {
        controller = spy(new ExternalIdController());
        controller.setExternalIdService(externalIdService);
        controller.setStudyService(studyService);
        
        User user = new User();
        user.setHealthCode("BBB");
        
        UserSession session = mock(UserSession.class);
        when(session.getUser()).thenReturn(user);
        when(session.getStudyIdentifier()).thenReturn(TestConstants.TEST_STUDY);
        
        doReturn(session).when(controller).getAuthenticatedSession(Roles.DEVELOPER);

        study = new DynamoStudy();
        when(studyService.getStudy(TestConstants.TEST_STUDY)).thenReturn(study);
        
        TestUtils.mockPlayContext();
    }

    @Test
    public void getExternalIds() throws Exception {
        // Mock out a response from service
        PagedResourceList<ExternalIdentifierInfo> page = new PagedResourceList<>(EXT_IDS, null, 5, 10)
                .withOffsetKey("CCC")
                .withFilter("idFilter", "A");
        when(externalIdService.getExternalIds(any(), any(), any(), any(), any())).thenReturn(page);
        
        // execute the controller
        Result result = controller.getExternalIds(null, null, null, null);
        String content = Helpers.contentAsString(result);
        
        System.out.println(content);
        
        PagedResourceList<ExternalIdentifierInfo> deserPage =  MAPPER.readValue(content, PAGE_REF);
        assertEquals(EXT_IDS, deserPage.getItems());
        assertEquals("CCC", deserPage.getOffsetKey());
        assertEquals(5, deserPage.getPageSize());
        assertEquals(10, deserPage.getTotal());
        assertEquals("A", deserPage.getFilters().get("idFilter"));
    }
    
    @Test
    public void addExternalIds() throws Exception {
        List<String> identifiers = Lists.newArrayList("AAA", "BBB", "CCC");
        TestUtils.mockPlayContextWithJson(MAPPER.writeValueAsString(identifiers));
        
        Result result = controller.addExternalIds();
        JsonNode node = MAPPER.readTree(Helpers.contentAsString(result));
        String message = node.get("message").asText();
        
        assertEquals("External identifiers added.", message);
        assertEquals(201, result.status());
        
        verify(externalIdService).addExternalIds(study, identifiers);
    }
    
    @Test
    public void noIdentifiers() throws Exception {
        TestUtils.mockPlayContextWithJson("[]");
        
        Result result = controller.addExternalIds();
        JsonNode node = MAPPER.readTree(Helpers.contentAsString(result));
        String message = node.get("message").asText();
        
        assertEquals("External identifiers added.", message);
        assertEquals(201, result.status());
        
        verify(externalIdService).addExternalIds(study, Lists.newArrayList());
    }
    
}
