import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.project.Project
import com.atlassian.jira.project.ProjectManager
import com.atlassian.jira.security.roles.ProjectRoleManager
import com.atlassian.jira.security.roles.ProjectRoleActors
import com.atlassian.jira.security.roles.ProjectRoleActor
import com.atlassian.jira.security.roles.ProjectRole
import com.atlassian.jira.security.roles.RoleActor
import com.atlassian.jira.security.roles.RoleActorFactory
//import com.atlassian.jira.bc.projectroles.ProjectRoleService; // This will not work until JMWE 5.1.0, known issue
import com.atlassian.crowd.embedded.api.Group
import org.apache.log4j.Category
import org.apache.log4j.Logger
import org.apache.log4j.Level
import com.atlassian.jira.ComponentManager
import com.atlassian.jira.util.SimpleErrorCollection
import java.util.*

def groupManager = ComponentAccessor.getGroupManager()
def projectManager = ComponentAccessor.getProjectManager() // for later implementation
def projectRoleManager = ComponentAccessor.getComponentOfType(ProjectRoleManager.class) as ProjectRoleManager
def errorCollection = new SimpleErrorCollection() //testing
//def projectRoleService = ComponentAccessor.getComponent(ProjectRoleService) 
// This will not work until JMWE 5.1.0, known issue, below is suggested fix
def projectRoleService = ComponentAccessor.getComponent(ComponentAccessor.getClassLoader().findClass("com.atlassian.jira.bc.projectroles.ProjectRoleService")) 
 
// Get list of all projects
List<Project> projects = projectManager.getProjectObjects()

// Create our ArrayList of the JIRA system groups we don't want to mess with
ArrayList<String> excludedGroups = new ArrayList<String>()
excludedGroups.add("jira-administrators")
excludedGroups.add("jira-users")
excludedGroups.add("jira-software-users")
excludedGroups.add("jira-servicedesk-users")
excludedGroups.add("jira-servicedesk-administrators")
excludedGroups.add("jira-servicedesk-users-1")
excludedGroups.add("jira-core-users")

// Might eliminiate this, unused and unlikely to be used
ArrayList<String> neededGroupNames = new ArrayList<String>()
neededGroupNames.add("-Administrator")
neededGroupNames.add("-Developers")
neededGroupNames.add("-Users")
neededGroupNames.add("-Vendors")

// Get the 4 project roles we're concerned about
ProjectRole adminRole = projectRoleManager.getProjectRole("Administrators")
ProjectRole devRole = projectRoleManager.getProjectRole("Developer")
ProjectRole userRole = projectRoleManager.getProjectRole("User")
ProjectRole vendorRole = projectRoleManager.getProjectRole("Vendor")


// Iterate through all projects
for (def project: projects)
{
    log.debug("Project key is: " + project.getKey())
  

	// Get the group object associated with the group, or NULL if the group DNE
  	Group adminGroup = groupManager.getGroup(project.getKey() + "-Administrator")
    Group devsGroup = groupManager.getGroup(project.getKey() + "-Developers")
    Group usersGroup = groupManager.getGroup(project.getKey() + "-Users")
    Group vendorsGroup = groupManager.getGroup(project.getKey() + "-Vendors")
  
	// These four if statements will make sure all relevant groups have been created.  
  	// If (whatever)Group is null, it means the group doesn't exist and we need to create it
    if(adminGroup == null)
  	{
      log.debug("there is no admin group in project " + project.getKey())
      adminGroup = groupManager.createGroup(project.getKey() + "-Administrator")
      log.debug("The group has been created")
    }
  
    if(devsGroup == null)
    {
      log.debug("there is no devs group in project " + project.getKey())
      devsGroup = groupManager.createGroup(project.getKey() + "-Developers")
      log.debug("The group has been created")      
    }
  
    if(usersGroup == null)
    {
      log.debug("there is no users group in project " + project.getKey())
      usersGroup = groupManager.createGroup(project.getKey() + "-Users")
      log.debug("The group has been created")
    }
  
    if(vendorsGroup == null)
    {
      log.debug("there is no vendors group in project " + project.getKey())
      vendorsGroup = groupManager.createGroup(project.getKey() + "-Vendors")
      log.debug("The group has been created")      
    }
  
  	// Get all user and group actors in project role, and add that to the ArrayList
    // so that we can later iterate over all roles
	ArrayList<ProjectRoleActors> allProjectRoleActors = new ArrayList<ProjectRoleActors>()
  	allProjectRoleActors.add(projectRoleManager.getProjectRoleActors(adminRole,project))
  	allProjectRoleActors.add(projectRoleManager.getProjectRoleActors(devRole,project))
  	allProjectRoleActors.add(projectRoleManager.getProjectRoleActors(userRole,project))
  	allProjectRoleActors.add(projectRoleManager.getProjectRoleActors(vendorRole,project))

	
	// Create some boolean logic variables to check if groups have been added to roles
	boolean admin   = false
	boolean devs    = false
	boolean users   = false
	boolean vendors = false	
	int roleIndex = 1  // Index for iterating through the following for... loop, 1=admin,2=devs,3=users,4=vendors
	
  	for(def projectRoleActor: allProjectRoleActors)
  	{
    
	  // Get all group and user actors (users unassigned to groups) in project admin role
      def currentGroupRoleActor = projectRoleActor.getRoleActorsByType("atlassian-group-role-actor")
      def currentUserRoleActor = projectRoleActor.getRoleActorsByType("atlassian-user-role-actor")  

      // For each group listed in the role
      for(def roleActor: currentGroupRoleActor)
      {
		  
		//log.debug("Current descriptor is: " + roleActor.getDescriptor())
        // Is group one of the seven excluded groups? If so, ignore
        if(excludedGroups.contains(roleActor.getDescriptor()))
        {
          //log.debug("Ignoring group: " + roleActor.getDescriptor())
        }
        // Begin checking to see if the right groups have been added to the right roles
        else if(roleActor.getDescriptor().equals(project.getKey()+"-Administrator"))
        {
          admin = true  // the Administrators group already exists in the Admin role
          log.debug("the Administrators group already exists in the Admin role")
        }
        else if(roleActor.getDescriptor().equals(project.getKey()+"-Developers"))
        {
          devs = true  // the Developers group already exists in the Developer role
          log.debug("the Developers group already exists in the Developer role")
        }
        else if(roleActor.getDescriptor().equals(project.getKey()+"-Users"))
        {
          users = true  // the Users group already exists in the User role
          log.debug("the Users group already exists in the User role")
        }
        else if(roleActor.getDescriptor().equals(project.getKey()+"-Vendors"))
        {
          vendors = true  // the Vendors group already exists in the Vendor role
          log.debug("the Vendors group already exists in the Vendor role")
        }		
		/* TODO: Remove this section if no use is found
        // Will only execute if group is not a excluded group or group management group
        else
        { 
          //log.debug("Group Name: " + roleActor.getDescriptor())
          for(def user: roleActor.getUsers())
          {
            //log.debug("Username: " + user.getName())
          }
        }*/
      }
		// We're going to add the groups to the relevant role if they aren't there already
		// If the Administrator group does not exist in the Administrators role, we add it
		if(admin == false && roleIndex == 1)
		{
		  log.debug(project.getKey() + "-Administrator does not exist, attempting to add it.")
		  projectRoleService.addActorsToProjectRole([project.getKey() + "-Administrator"],
													adminRole,
													project,
													ProjectRoleActor.GROUP_ROLE_ACTOR_TYPE,
													errorCollection)
		}	

		// Now, if the Developers group does not exist in the Developer role, we add it
		if(devs == false && roleIndex == 2)
		{
		  log.debug(project.getKey() + "-Developers does not exist, attempting to add it.")
		  projectRoleService.addActorsToProjectRole([project.getKey() + "-Developers"],
													devRole,
													project,
													ProjectRoleActor.GROUP_ROLE_ACTOR_TYPE,
													errorCollection)
		}		

		// Now, if the Users group does not exist in the User role, we add it
		if(users == false && roleIndex == 3)
		{
		  log.debug(project.getKey() + "-Users does not exist, attempting to add it.")
		  projectRoleService.addActorsToProjectRole([project.getKey() + "-Users"],
													userRole,
													project,
													ProjectRoleActor.GROUP_ROLE_ACTOR_TYPE,
													errorCollection)
		}

		// Now, if the Vendors group does not exist in the Vendor role, we add it
		if(vendors == false && roleIndex == 4)
		{
		  log.debug(project.getKey() + "-Vendors does not exist, attempting to add it.")
		  projectRoleService.addActorsToProjectRole([project.getKey() + "-Vendors"],
													vendorRole,
													project,
													ProjectRoleActor.GROUP_ROLE_ACTOR_TYPE,
													errorCollection)
		}	  
	  int n = 1
	  // Now we'll handle users that are not inside any group, and add them to the correct group
	  for(def roleActor: currentUserRoleActor)
	  {
		//log.debug("Role Actor Type is: " + roleActor.getType() + " with description " + roleActor.getDescriptor())
		//log.debug(n.toString())
		// For each user listed
		for(def user: roleActor.getUsers())
		{


				//log.debug("Username: " + user.getName() + " and User Key: " + user.getKey())
			// Check to make sure user is an active user
			if(user.isActive() == false)
			{
				//log.debug("Username: " + user.getName() + " is not an active user. Removing user from role")
				
				if(user.getKey().equals("bernholde@ornl.gov"))
				{
					log.debug("YABE (Yet Another Bernholde Error)")
				}
				else if(roleIndex == 1)
				{
					projectRoleService.removeActorsFromProjectRole([user.getKey()],
														adminRole,
														project,
														ProjectRoleActor.USER_ROLE_ACTOR_TYPE,
														errorCollection)
					log.debug("User: " + user.getKey() + " is not an active user. Removing from Admin role")
				}
				else if(roleIndex == 2)
				{
					groupManager.addUserToGroup(user,groupManager.getGroup(project.getKey() + "-Developers"))
					projectRoleService.removeActorsFromProjectRole([user.getKey()],
														devRole,
														project,
														ProjectRoleActor.USER_ROLE_ACTOR_TYPE,
														errorCollection)
					log.debug("User: " + user.getKey() + " is not an active user. Removing from Dev role")													
				}
				else if(roleIndex == 3)
				{
					groupManager.addUserToGroup(user,groupManager.getGroup(project.getKey() + "-Users"))
					projectRoleService.removeActorsFromProjectRole([user.getKey()],
														userRole,
														project,
														ProjectRoleActor.USER_ROLE_ACTOR_TYPE,
														errorCollection)
					log.debug("User: " + user.getKey() + " is not an active user. Removing from User role")													
				}
				else if(roleIndex == 4)
				{
					groupManager.addUserToGroup(user,groupManager.getGroup(project.getKey() + "-Vendors"))
					projectRoleService.removeActorsFromProjectRole([user.getKey()],
														vendorRole,
														project,
														ProjectRoleActor.USER_ROLE_ACTOR_TYPE,
														errorCollection)
					log.debug("User: " + user.getKey() + " is not an active user. Removing from Vendor role")													
				}
				else
				{
					log.debug("Something has gone terribly wrong while adding users to groups!")
				}				
			}
			else
			{
				if(roleIndex == 1)
				{
					groupManager.addUserToGroup(user,groupManager.getGroup(project.getKey() + "-Administrator"))
					projectRoleService.removeActorsFromProjectRole([user.getKey()],
														adminRole,
														project,
														ProjectRoleActor.USER_ROLE_ACTOR_TYPE,
														errorCollection)
					log.debug("Adding user: " + user.getKey() + " to group " + project.getKey() + "-Administrator and removing from Admin role")
				}
				else if(roleIndex == 2)
				{
					groupManager.addUserToGroup(user,groupManager.getGroup(project.getKey() + "-Developers"))
					projectRoleService.removeActorsFromProjectRole([user.getKey()],
														devRole,
														project,
														ProjectRoleActor.USER_ROLE_ACTOR_TYPE,
														errorCollection)
					log.debug("Adding user: " + user.getKey() + " to group " + project.getKey() + "-Developers and removing from Dev role")													
				}
				else if(roleIndex == 3)
				{
					groupManager.addUserToGroup(user,groupManager.getGroup(project.getKey() + "-Users"))
					projectRoleService.removeActorsFromProjectRole([user.getKey()],
														userRole,
														project,
														ProjectRoleActor.USER_ROLE_ACTOR_TYPE,
														errorCollection)
					log.debug("Adding user: " + user.getKey() + " to group " + project.getKey() + "-Users and removing from User role")													
				}
				else if(roleIndex == 4)
				{
					groupManager.addUserToGroup(user,groupManager.getGroup(project.getKey() + "-Vendors"))
					projectRoleService.removeActorsFromProjectRole([user.getKey()],
														vendorRole,
														project,
														ProjectRoleActor.USER_ROLE_ACTOR_TYPE,
														errorCollection)
					log.debug("Adding user: " + user.getKey() + " to group " + project.getKey() + "-Vendors and removing from Vendor role")													
				}
				else
				{
					log.debug("Something has gone terribly wrong while adding users to groups!")
				}
			}
		}
		n++
	  }
	  roleIndex++ // increment the index to move to next role
  	}

}
