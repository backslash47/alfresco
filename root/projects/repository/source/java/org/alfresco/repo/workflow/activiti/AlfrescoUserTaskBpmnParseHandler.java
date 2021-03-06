/* Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.alfresco.repo.workflow.activiti;

import java.util.ArrayList;
import java.util.List;

import org.activiti.bpmn.model.BaseElement;
import org.activiti.bpmn.model.UserTask;
import org.activiti.engine.delegate.TaskListener;
import org.activiti.engine.impl.bpmn.behavior.UserTaskActivityBehavior;
import org.activiti.engine.impl.bpmn.parser.BpmnParse;
import org.activiti.engine.impl.bpmn.parser.handler.AbstractBpmnParseHandler;
import org.activiti.engine.impl.pvm.delegate.ActivityBehavior;
import org.activiti.engine.impl.pvm.process.ActivityImpl;
import org.activiti.engine.parse.BpmnParseHandler;

/**
 * A {@link BpmnParseHandler} that adds execution listeners to a
 * {@link UserTask} which are specifically for Alfresco usage.
 * 
 * @author Joram Barrez
 * @author Frederik Heremans
 * @author Nick Smith
 */
public class AlfrescoUserTaskBpmnParseHandler extends AbstractBpmnParseHandler<UserTask>
{
    private TaskListener      completeTaskListener;
    private TaskListener      createTaskListener;

    protected Class<? extends BaseElement> getHandledType()
    {
        return UserTask.class;
    }

    protected void executeParse(BpmnParse bpmnParse, UserTask userTask)
    {
        ActivityImpl activity = findActivity(bpmnParse, userTask.getId());
        ActivityBehavior activitybehaviour = activity.getActivityBehavior();
        if (activitybehaviour instanceof UserTaskActivityBehavior)
        {
            UserTaskActivityBehavior userTaskActivity = (UserTaskActivityBehavior) activitybehaviour;
            if (createTaskListener != null)
            {
            	addTaskListenerAsFirst(createTaskListener, TaskListener.EVENTNAME_CREATE, userTaskActivity);
            }
            if (completeTaskListener != null)
            {
            	addTaskListenerAsFirst(completeTaskListener, TaskListener.EVENTNAME_COMPLETE, userTaskActivity);
            }
        }
    }
    
    public void setCompleteTaskListener(TaskListener completeTaskListener)
    {
        this.completeTaskListener = completeTaskListener;
    }

    public void setCreateTaskListener(TaskListener createTaskListener)
    {
        this.createTaskListener = createTaskListener;
    }
    
    protected void addTaskListenerAsFirst(TaskListener taskListener, String eventName, UserTaskActivityBehavior userTask) {
    	List<TaskListener> taskEventListeners = userTask.getTaskDefinition().getTaskListeners().get(eventName);
        if (taskEventListeners == null) {
          taskEventListeners = new ArrayList<TaskListener>();
          userTask.getTaskDefinition().getTaskListeners().put(eventName, taskEventListeners);
        }
        taskEventListeners.add(0, taskListener);
    }
}