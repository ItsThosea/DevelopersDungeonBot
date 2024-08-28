package me.thosea.developersdungeon.button;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public interface ButtonHandler {
	String ID_MAKE_CHANNEL = "devdungeon_makechannel";
	String ID_EDIT_STATUS = "devdungeon_editstatus";

	String ID_PCHANNEL_HELP = "devdungeon_channel_help";
	String ID_PCHANNEL_DELETE = "devdungeon_channel_delete";
	String ID_PCHANNEL_LEAVE = "devdungeon_channel_leave";

	String ID_DELETE_TEAM_ROLE = "devdungeon_deleteteamrole";
	String ID_JOIN_TEAM_ROLE = "devdungeon_jointeamrole";
	String ID_LEAVE_TEAM_ROLE = "devdungeon_leaveteamrole";
	String ID_TAKE_TEAM_OWNERSHIP = "devdungeon_taketeamownership";
	String ID_DENY_TEAM_REQUEST = "devdungeon_denyteamrequest";

	String ID_CONTENT_CREATOR_APP = "devdungeon_content_creator_app";

	String ID_TEAM_LIST_PAGE = "devdungeon_teamlist_page";

	String ID_SMP_SUGGEST_CONFIRM = "devdungeon_smp_suggest_confirm";

	static Map<String, ButtonHandler> makeHandlers() {
		List<ButtonHandler> handlers = List.of(
				new ButtonMakeChannel(),
				new ButtonEditCommissionStatus(),
				new ButtonPChannelHelp(),
				new ButtonDeletePChannel(),
				new ButtonDeleteTeamRole(),
				new ButtonJoinTeamRole(),
				new ButtonLeaveTeamRole(),
				new ButtonTakeTeamRoleOwnership(),
				new ButtonDenyTeamRequest(),
				new ButtonContentCreatorApp(),
				new ButtonTeamListPage(),
				new ButtonSmpSuggestConfirm(),
				new ButtonPChannelLeave()
		);

		Map<String, ButtonHandler> map = new HashMap<>(handlers.size());
		for(ButtonHandler handler : handlers) {
			map.put(handler.getId(), handler);
		}
		return map;
	}

	String getId();
	void handle(Member member, ButtonInteractionEvent event, String[] args);
}