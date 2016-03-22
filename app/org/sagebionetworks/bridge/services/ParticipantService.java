package org.sagebionetworks.bridge.services;

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.sagebionetworks.bridge.BridgeConstants.API_MAXIMUM_PAGE_SIZE;
import static org.sagebionetworks.bridge.BridgeConstants.API_MINIMUM_PAGE_SIZE;
import static org.sagebionetworks.bridge.dao.ParticipantOption.DATA_GROUPS;
import static org.sagebionetworks.bridge.dao.ParticipantOption.EMAIL_NOTIFICATIONS;
import static org.sagebionetworks.bridge.dao.ParticipantOption.EXTERNAL_IDENTIFIER;
import static org.sagebionetworks.bridge.dao.ParticipantOption.LANGUAGES;
import static org.sagebionetworks.bridge.dao.ParticipantOption.SHARING_SCOPE;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import org.sagebionetworks.bridge.dao.AccountDao;
import org.sagebionetworks.bridge.dao.ParticipantOption.SharingScope;
import org.sagebionetworks.bridge.exceptions.BadRequestException;
import org.sagebionetworks.bridge.models.PagedResourceList;
import org.sagebionetworks.bridge.models.accounts.Account;
import org.sagebionetworks.bridge.models.accounts.AccountSummary;
import org.sagebionetworks.bridge.models.accounts.HealthId;
import org.sagebionetworks.bridge.models.accounts.ParticipantOptionsLookup;
import org.sagebionetworks.bridge.models.accounts.StudyParticipant2;
import org.sagebionetworks.bridge.models.accounts.UserConsentHistory;
import org.sagebionetworks.bridge.models.accounts.UserProfile;
import org.sagebionetworks.bridge.models.studies.Study;
import org.sagebionetworks.bridge.models.subpopulations.Subpopulation;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;

@Component
public class ParticipantService {

    private static final String PAGE_SIZE_ERROR = "pageSize must be from "+API_MINIMUM_PAGE_SIZE+"-"+API_MAXIMUM_PAGE_SIZE+" records";
    
    private static final List<UserConsentHistory> NO_HISTORY = ImmutableList.of();
    
    private AccountDao accountDao;
    
    private ParticipantOptionsService optionsService;
    
    private SubpopulationService subpopService;
    
    private HealthCodeService healthCodeService;
    
    private ConsentService consentService;
    
    @Autowired
    final void setAccountDao(AccountDao accountDao) {
        this.accountDao = accountDao;
    }
    
    @Autowired
    final void setParticipantOptionsService(ParticipantOptionsService optionsService) {
        this.optionsService = optionsService;
    }
    
    @Autowired
    final void setSubpopulationService(SubpopulationService subpopService) {
        this.subpopService = subpopService;
    }
    
    @Autowired
    final void setHealthCodeService(HealthCodeService healthCodeService) {
        this.healthCodeService = healthCodeService;
    }
    
    @Autowired
    final void setUserConsent(ConsentService consentService) {
        this.consentService = consentService;
    }
    
    public StudyParticipant2 getParticipant(Study study, String email) {
        checkNotNull(study);
        checkArgument(isNotBlank(email));
        
        StudyParticipant2.Builder participant = new StudyParticipant2.Builder();
        
        Account account = accountDao.getAccount(study, email);
        String healthCode = getHealthCode(account);

        List<Subpopulation> subpopulations = subpopService.getSubpopulations(study.getStudyIdentifier());
        for (Subpopulation subpop : subpopulations) {
            if (healthCode != null) {
                // always returns a list, even if empty
                List<UserConsentHistory> history = consentService.getUserConsentHistory(study, subpop.getGuid(), healthCode, email);
                participant.addConsentHistory(subpop.getGuid(), history);
            } else {
                // Create an empty history if there's no health Code.
                participant.addConsentHistory(subpop.getGuid(), NO_HISTORY);
            }
        }
        // Accounts exist that have signatures but no health codes (when created but email is 
        // never verified, for example). Do not want roster generation to fail because of this,
        // so we just skip things that require the healthCode.
        if (healthCode != null) {
            ParticipantOptionsLookup lookup = optionsService.getOptions(healthCode);
            participant.withSharingScope(lookup.getEnum(SHARING_SCOPE, SharingScope.class));
            participant.withNotifyByEmail(lookup.getBoolean(EMAIL_NOTIFICATIONS));
            participant.withExternalId(lookup.getString(EXTERNAL_IDENTIFIER));
            participant.withDataGroups(lookup.getStringSet(DATA_GROUPS));
            participant.withLanguages(lookup.getOrderedStringSet(LANGUAGES));
        }
        participant.withFirstName(account.getFirstName());
        participant.withLastName(account.getLastName());
        participant.withEmail(account.getEmail());
        participant.withRoles(account.getRoles());
        
        Map<String,String> attributes = Maps.newHashMap();
        for (String attribute : study.getUserProfileAttributes()) {
            String value = account.getAttribute(attribute);
            attributes.put(attribute, value);
        }
        participant.withAttributes(attributes);
        
        if (study.isHealthCodeExportEnabled()) {
            participant.withHealthCode(healthCode);
        }
        return participant.build();
    }
    
    public PagedResourceList<AccountSummary> getPagedAccountSummaries(Study study, int offsetBy, int pageSize, String emailFilter) {
        checkNotNull(study);
        if (offsetBy < 0) {
            throw new BadRequestException("offsetBy cannot be less than 0");
        }
        // Just set a sane upper limit on this.
        if (pageSize < API_MINIMUM_PAGE_SIZE || pageSize > API_MAXIMUM_PAGE_SIZE) {
            throw new BadRequestException(PAGE_SIZE_ERROR);
        }
        return accountDao.getPagedAccountSummaries(study, offsetBy, pageSize, emailFilter);
    }
    
    /**
     * Update the fields of a user profile that can be updated, on behalf of a user (using an 
     * email address only).
     */
    public void updateProfile(Study study, String email, UserProfile profile) {
        Account account = accountDao.getAccount(study, email);
        account.setFirstName(profile.getFirstName());
        account.setLastName(profile.getLastName());
        for(String attribute : study.getUserProfileAttributes()) {
            String value = profile.getAttribute(attribute);
            account.setAttribute(attribute, value);
        }
        accountDao.updateAccount(study, account);
    }
    
    private String getHealthCode(Account account) {
        if (account.getHealthId() != null) {
            HealthId healthId = healthCodeService.getMapping(account.getHealthId());
            if (healthId != null && healthId.getCode() != null) {
                return healthId.getCode();
            }
        }
        return null;
    }
    
}