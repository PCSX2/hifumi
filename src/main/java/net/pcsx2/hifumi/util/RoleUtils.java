// SPDX-FileCopyrightText: 2026 PCSX2 Dev Team
// SPDX-License-Identifier: MIT
package net.pcsx2.hifumi.util;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;

public class RoleUtils {

    /**
     * Check if a member has a Role by ID
     * 
     * @param member - The Member to check
     * @param roleId  - The role to look for
     * @return True if the Member has the Role specified by ID, false otherwise.
     */
    public static boolean memberHasRole(Member member, String roleId) {
        for (Role role : member.getRoles()) {
            if (role.getId().equals(roleId)) {
                return true;
            }
        }

        return false;
    }

    public static boolean memberHasRole(Member member, long roleIdLong) {
        return memberHasRole(member, String.valueOf(roleIdLong));
    }
}
