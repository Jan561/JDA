/*
 * Copyright 2015-2019 Austin Keener, Michael Ritter, Florian Spieß, and the JDA contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.dv8tion.jda.internal.managers;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Emote;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import net.dv8tion.jda.api.managers.EmoteManager;
import net.dv8tion.jda.api.utils.json.DataObject;
import net.dv8tion.jda.internal.entities.EmoteImpl;
import net.dv8tion.jda.internal.requests.Route;
import net.dv8tion.jda.internal.utils.Checks;
import okhttp3.RequestBody;

import javax.annotation.CheckReturnValue;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class EmoteManagerImpl extends ManagerBase<EmoteManager> implements EmoteManager
{
    protected final EmoteImpl emote;

    protected final List<String> roles = new ArrayList<>();
    protected String name;

    /**
     * Creates a new EmoteManager instance
     *
     * @param  emote
     *         The target {@link net.dv8tion.jda.internal.entities.EmoteImpl EmoteImpl} to modify
     *
     * @throws java.lang.IllegalStateException
     *         If the specified Emote is {@link net.dv8tion.jda.api.entities.Emote#isFake() fake} or {@link net.dv8tion.jda.api.entities.Emote#isManaged() managed}.
     */
    public EmoteManagerImpl(EmoteImpl emote)
    {
        super(emote.getJDA(), Route.Emotes.MODIFY_EMOTE.compile(notNullGuild(emote).getId(), emote.getId()));
        this.emote = emote;
        if (isPermissionChecksEnabled())
            checkPermissions();
    }

    private static Guild notNullGuild(EmoteImpl emote)
    {
        Guild g = emote.getGuild();
        if (g == null)
            throw new IllegalStateException("Cannot modify a fake emote");
        return g;
    }

    @Override
    public Emote getEmote()
    {
        return emote;
    }

    @Override
    @CheckReturnValue
    public EmoteManagerImpl reset(long fields)
    {
        super.reset(fields);
        if ((fields & ROLES) == ROLES)
            withLock(this.roles, List::clear);
        if ((fields & NAME) == NAME)
            this.name = null;
        return this;
    }

    @Override
    @CheckReturnValue
    public EmoteManagerImpl reset(long... fields)
    {
        super.reset(fields);
        return this;
    }

    @Override
    @CheckReturnValue
    public EmoteManagerImpl reset()
    {
        super.reset();
        withLock(this.roles, List::clear);
        this.name = null;
        return this;
    }

    @Override
    @CheckReturnValue
    public EmoteManagerImpl setName(String name)
    {
        Checks.notBlank(name, "Name");
        Checks.check(name.length() >= 2 && name.length() <= 32, "Name must be between 2-32 characters long");
        this.name = name;
        set |= NAME;
        return this;
    }

    @Override
    @CheckReturnValue
    public EmoteManagerImpl setRoles(Set<Role> roles)
    {
        if (roles == null)
        {
            withLock(this.roles, List::clear);
        }
        else
        {
            Checks.notNull(roles, "Roles");
            roles.forEach((role) ->
            {
                Checks.notNull(role, "Roles");
                Checks.check(role.getGuild().equals(getGuild()), "Roles must all be from the same guild");
            });
            withLock(this.roles, (list) ->
            {
                list.clear();
                roles.stream().map(Role::getId).forEach(list::add);
            });
        }
        set |= ROLES;
        return this;
    }

    @Override
    protected RequestBody finalizeData()
    {
        DataObject object = DataObject.empty();
        if (shouldUpdate(NAME))
            object.put("name", name);
        withLock(this.roles, (list) ->
        {
            if (shouldUpdate(ROLES))
                object.put("roles", list);
        });

        reset();
        return getRequestBody(object);
    }

    @Override
    protected boolean checkPermissions()
    {
        if (!getGuild().getSelfMember().hasPermission(Permission.MANAGE_EMOTES))
            throw new InsufficientPermissionException(Permission.MANAGE_EMOTES);
        return super.checkPermissions();
    }
}
