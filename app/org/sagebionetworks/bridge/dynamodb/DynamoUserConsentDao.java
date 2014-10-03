package org.sagebionetworks.bridge.dynamodb;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.sagebionetworks.bridge.dao.UserConsentDao;
import org.sagebionetworks.bridge.exceptions.EntityAlreadyExistsException;
import org.sagebionetworks.bridge.exceptions.EntityNotFoundException;
import org.sagebionetworks.bridge.models.ConsentSignature;
import org.sagebionetworks.bridge.models.StudyConsent;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapperConfig;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapperConfig.ConsistentReads;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapperConfig.SaveBehavior;
import com.amazonaws.services.dynamodbv2.model.ConditionalCheckFailedException;

public class DynamoUserConsentDao implements UserConsentDao {

    private DynamoDBMapper mapper;

    public void setDynamoDbClient(AmazonDynamoDB client) {
        DynamoDBMapperConfig mapperConfig = new DynamoDBMapperConfig(
                SaveBehavior.UPDATE_SKIP_NULL_ATTRIBUTES,
                ConsistentReads.CONSISTENT,
                TableNameOverrideFactory.getTableNameOverride(DynamoUserConsent2.class));
        mapper = new DynamoDBMapper(client, mapperConfig);
    }

    @Override
    public void giveConsent(String healthCode, StudyConsent consent, ConsentSignature consentSignature) {
        giveConsentNew(healthCode, consent, consentSignature);
    }

    @Override
    public void withdrawConsent(String healthCode, StudyConsent consent) {
        withdrawConsentNew(healthCode, consent);
    }

    @Override
    public Long getConsentCreatedOn(String healthCode, String studyKey) {
        return getConsentCreatedOnNew(healthCode, studyKey);
    }

    @Override
    public boolean hasConsented(String healthCode, StudyConsent consent) {
        boolean hasConsented = hasConsentedNew(healthCode, consent);
        return hasConsented;
    }

    @Override
    public ConsentSignature getConsentSignature(String healthCode, StudyConsent consent) {
        ConsentSignature signature = getConsentSignatureNew(healthCode, consent);
        return signature;
    }

    @Override
    public void resumeSharing(String healthCode, StudyConsent consent) {
        DynamoUserConsent2 userConsent = new DynamoUserConsent2(healthCode, consent);
        userConsent = mapper.load(userConsent);
        if (userConsent == null) {
            throw new EntityNotFoundException(DynamoUserConsent2.class);
        }
        userConsent.setDataSharing(true);
        mapper.save(userConsent);
    }

    @Override
    public void suspendSharing(String healthCode, StudyConsent consent) {
        DynamoUserConsent2 userConsent = new DynamoUserConsent2(healthCode, consent);
        userConsent = mapper.load(userConsent);
        if (userConsent == null) {
            throw new EntityNotFoundException(DynamoUserConsent2.class);
        }
        userConsent.setDataSharing(false);
        mapper.save(userConsent);
    }

    @Override
    public boolean isSharingData(String healthCode, StudyConsent consent) {
        DynamoUserConsent2 userConsent = new DynamoUserConsent2(healthCode, consent);
        userConsent = mapper.load(userConsent);
        return (userConsent != null && userConsent.getDataSharing());
    }

    void giveConsentNew(String healthCode, StudyConsent studyConsent, ConsentSignature researchConsent) {
        DynamoUserConsent2 consent = null;
        try {
            consent = new DynamoUserConsent2(healthCode, studyConsent);
            consent = mapper.load(consent);
            if (consent == null) { // If the user has not consented yet
                consent = new DynamoUserConsent2(healthCode, studyConsent);
            }
            consent.setName(researchConsent.getName());
            consent.setBirthdate(researchConsent.getBirthdate());
            consent.setSignedOn(DateTime.now(DateTimeZone.UTC).getMillis());
            consent.setDataSharing(true);
            mapper.save(consent);
        } catch (ConditionalCheckFailedException e) {
            throw new EntityAlreadyExistsException(consent);
        }
    }

    void withdrawConsentNew(String healthCode, StudyConsent studyConsent) {
        DynamoUserConsent2 consentToDelete = new DynamoUserConsent2(healthCode, studyConsent);
        consentToDelete = mapper.load(consentToDelete);
        if (consentToDelete == null) {
            return;
        }
        mapper.delete(consentToDelete);
    }

    Long getConsentCreatedOnNew(String healthCode, String studyKey) {
        DynamoUserConsent2 consent = new DynamoUserConsent2(healthCode, studyKey);
        consent = mapper.load(consent);
        return consent == null ? null : consent.getConsentCreatedOn();
    }

    boolean hasConsentedNew(String healthCode, StudyConsent studyConsent) {
        DynamoUserConsent2 consent = new DynamoUserConsent2(healthCode, studyConsent);
        return mapper.load(consent) != null;
    }

    ConsentSignature getConsentSignatureNew(String healthCode, StudyConsent studyConsent) {
        DynamoUserConsent2 consent = new DynamoUserConsent2(healthCode, studyConsent);
        consent = mapper.load(consent);
        if (consent == null) {
            throw new EntityNotFoundException(DynamoUserConsent2.class);
        }
        return new ConsentSignature(consent.getName(), consent.getBirthdate());
    }
}
