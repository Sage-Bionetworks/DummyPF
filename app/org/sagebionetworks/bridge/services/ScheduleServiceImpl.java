package org.sagebionetworks.bridge.services;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.sagebionetworks.bridge.BridgeUtils;
import org.sagebionetworks.bridge.dao.ScheduleDao;
import org.sagebionetworks.bridge.models.Study;
import org.sagebionetworks.bridge.models.User;
import org.sagebionetworks.bridge.models.schedules.Schedule;
import org.sagebionetworks.bridge.models.schedules.SchedulePlan;
import org.sagebionetworks.bridge.validators.Validate;
import org.springframework.validation.Validator;

public class ScheduleServiceImpl implements ScheduleService {
    
    private Validator validator;

    private ScheduleDao scheduleDao;
    
    public void setScheduleDao(ScheduleDao scheduleDao) {
        this.scheduleDao = scheduleDao;
    }
    
    public void setValidator(Validator validator) {
        this.validator = validator;
    }
    
    @Override
    public List<Schedule> getSchedules(Study study, User user) {
        checkNotNull(study, "Study cannot be null");
        checkNotNull(user, "User cannot be null");
        
        return scheduleDao.getSchedules(study, user);
    }

    @Override
    public List<Schedule> createSchedules(List<Schedule> schedules) {
        checkNotNull(schedules, "Schedules cannot be null");
        
        for (Schedule schedule : schedules) {
            schedule.setGuid(BridgeUtils.generateGuid());
            Validate.entityThrowingException(validator, schedule);
        }        
        return scheduleDao.createSchedules(schedules);
    }
    
    @Override
    public void deleteSchedules(SchedulePlan plan) {
        checkNotNull(plan, "Schedule plan cannot be null");
        checkArgument(StringUtils.isNotBlank(plan.getGuid()), "Schedule plan GUID cannot be blank or null");
        
        scheduleDao.deleteSchedules(plan);
    }
    
    @Override
    public void deleteSchedules(Study study, User user) {
        checkNotNull(study, "Study cannot be null");
        checkNotNull(user, "User cannot be null");
        
        scheduleDao.deleteSchedules(study, user);
    }
    
}