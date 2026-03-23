package net.pcsx2.hifumi.permissions;

public enum PermissionRole
{
    MESSAGE_LOG_BYPASS_ROLE("Hifumi MLB");
    
    private String roleName;
    
    private PermissionRole(String roleName)
    {
        this.roleName = roleName;
    }
    
    /**
     * Resolve this enum to the display name of the Role it is supposed to represent.
     * @return String value of the role's display name
     */
    @Override
    public String toString()
    {
        return this.roleName;
    }
}
