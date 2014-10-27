package org.sagebionetworks.bridge.models.schedules;

import org.sagebionetworks.bridge.TestConstants;
import org.sagebionetworks.bridge.dynamodb.DynamoSchedule;
import org.sagebionetworks.bridge.dynamodb.DynamoSchedulePlan;
import org.sagebionetworks.bridge.json.DateUtils;

public class TestABSchedulePlan extends DynamoSchedulePlan {

    private Schedule schedule1 = new DynamoSchedule() {
        {
            setGuid("AAA");
            setScheduleType(ScheduleType.RECURRING);
            setCronTrigger("* * *");
            setActivityType(ActivityType.TASK);
            setActivityRef("task:AAA");
            setExpires(new Long(60000));
            setLabel("Test label for the user");
        }
    };
    private Schedule schedule2 = new DynamoSchedule() {
        {
            setGuid("BBB");
            setScheduleType(ScheduleType.RECURRING);
            setCronTrigger("* * *");
            setActivityType(ActivityType.TASK);
            setActivityRef("task:BBB");
            setExpires(new Long(60000));
            setLabel("Test label for the user");
        }
    };
    private Schedule schedule3 = new DynamoSchedule() {
        {
            setGuid("CCC");
            setScheduleType(ScheduleType.RECURRING);
            setCronTrigger("* * *");
            setActivityType(ActivityType.TASK);
            setActivityRef("task:CCC");
            setExpires(new Long(60000));
            setLabel("Test label for the user");
        }
    };
    
    public TestABSchedulePlan() {
        setGuid("AAA");
        setModifiedOn(DateUtils.getCurrentMillisFromEpoch());
        setStudyKey(TestConstants.TEST_STUDY_KEY);
        
        ABTestScheduleStrategy strategy = new ABTestScheduleStrategy();
        strategy.addGroup(40, schedule1);
        strategy.addGroup(40, schedule2);
        strategy.addGroup(20, schedule3);
        setStrategy(strategy);
    }
    
}