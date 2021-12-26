package java.game;

import java.io.*;
import java.util.*;
import java.util.resource.*;
import java.render.*;	//Text
import java.render.osd.*;	//Text
import java.sound.*;

public class TestTrack extends Track
{
	final static int MPH_60 = 771.604938271604938271604938271605;
	final static int MPH_124 = 3086.41975308641975308641975308642;

	Vector3 posExit = new Vector3( -456,0.0,-584 );
	Ypr	oriExit = new Ypr( 3.0, 0.0, 0.0 );

	Text	messageTextA;
	Text	messageTextB;

	int	testStatus = 0;
	int	triggerIndex = 0;
	int	acceleration = 0;
	int	acceleration120 = 0;
	float	testStartTime = 0.0f;

	Round	actRound = null;
	Round[]	round = new Round[2];
	int	rounds = 2;

	//RAXAT: new in v2.3.1
	int	actGroup, defGroup, lapGroup, accelGroup, timerGroup;
	Osd	losd; //local OSD for rectangles and texts

	public TestTrack()
	{
		map = new GroundRef( maps.test_track:0x00000001r );
		nav = new Navigator( -5.317-2.64, -15.996, 2.64, maps.test_track.smallmap:0x00000001r, maps.test_track.smallmap:0x00000002r, maps.test_track.smallmap:0x00000005r, 6, 8, 10 );
	}

	public void enter( GameState prev_state )
	{
		losd = new Osd();
		losd.show();

		defGroup = losd.endGroup();

		float xpos = 0.775;
		float ypos = -0.7;

		//RAXAT: additional resolution-based adjustments for OSD groups
		ResourceRef charset = Frontend.largeFont;
		float spacing = 1-Text.getLineSpacing(charset, osd.vp)-0.925;

		losd.createRectangle(xpos+0.03, ypos+0.1, 0.31875, 0.5625, -1, new ResourceRef(frontend:0x0000940Ar)); //acceleration test banner
		losd.hideGroup(accelGroup = losd.endGroup());

		losd.createRectangle(xpos+0.025, ypos-0.05, 0.75, 0.20625, -1, new ResourceRef(frontend:0x0000940Cr)); //timer title
		messageTextA = losd.createText(null, charset, Text.ALIGN_LEFT, xpos-0.29, ypos-0.115+spacing);

		losd.createRectangle(xpos+0.025, ypos+0.15, 0.75, 0.20625, -1, new ResourceRef(frontend:0x0000940Cr)); //timer text
		messageTextB = losd.createText(null, charset, Text.ALIGN_LEFT, xpos-0.29, ypos+0.085+spacing);
		losd.hideGroup(timerGroup = losd.endGroup());

		losd.createRectangle(xpos+0.03, ypos+0.1, 0.31875, 0.5625, -1, new ResourceRef(frontend:0x0000940Br)); //lap time banner
		losd.hideGroup(lapGroup = losd.endGroup());

		Frontend.loadingScreen.show(new ResourceRef(frontend:0x000002E6r));
		GfxEngine.flush();

		if(prev_state instanceof Garage)
		{
			posStart=posExit;
			oriStart=oriExit;
		}

		//1/4 miles
		round[0] = new Round( this, accelGroup );
		round[0].startdir( 3.14 );
		round[0].point( 0.000, 0.000, -1461.785, 1, 10 );
		round[0].point( 0.000, 0.000, -1059.785, 1, 20 ); //quartermile
		round[0].point( 0.000, 0.000, 148.215, 1, 20 );	  //mile

		round[1] = new Round( this, lapGroup );
		round[1].startdir( 3.14 );
		round[1].point( -450.598, 0.000, -524.816, 1 );
		round[1].point( -450.598, 0.000, -223.142, 1 );
		round[1].point( -444.736, 1.011, 72.270, 1 );
		round[1].point( -27.535, 1.425, 450.475, 1 );
		round[1].point( 444.689, 1.011, 72.560, 1 );
		round[1].point( 450.598, 0.000, -223.142, 1 );
		round[1].point( 450.598, 0.000, -544.816, 1 );
		round[1].point( 450.598, 0.000, -893.995, 1 );
		round[1].point( 446.293, 0.965, -1124.731, 1 );
		round[1].point( -40.000, 1.425, -1513.328, 1 );	
		round[1].point( -446.000, 1.425, -1124.000, 1 );
		round[1].point( -450.598, 0.000, -893.996, 1 );
		round[1].loop();

		actRound = null;

		super.enter(prev_state);

		Integrator.isCity = 0;
		name = "Test track";
	}

	public void exit( GameState next_state )
	{
        	if(!(next_state instanceof RaceSetup)) removeAllTimers();

		losd.hide();
		losd=null;

		if( round[0] )
		{
			round[0].destroy();
			round[0] = null;
		}
		if( round[1] )
		{
			round[1].destroy();
			round[1] = null;
		}

		super.exit( next_state );
	}

	public void event_handlerTrigger( GameRef obj_ref, int event, String param )
	{
		int id = param.token(0).intValue();
		if (id == player.car.id())
		{
			if( event == EVENT_TRIGGER_ON )
			{
				if( !activeTrigger )
				{
					activeTrigger=obj_ref.id();
				}
			}
			else
			{
				activeTrigger=0;
			}
		}
	}

	public void handleEvent( GameRef obj_ref, int event, int param )
	{
		super.handleEvent( obj_ref, event, param );

		if( event == EVENT_TIME )
		{
			if( param == 2 ) //one sec tick
			{
				if( activeTrigger )
				{
					if( player.car.getSpeedSquare() < 0.25 ) //slow speed?
					{	
						for( int i = 0; i < rounds; i++ )
						{
							if( round[i] )
							{
								if( activeTrigger == round[i].trigger.elementAt(0).trigger.id() )
								{
									Ypr ypr = player.car.getOri();

									float diff = ypr.y-round[i].startOri.y;
									if( diff >=  3.14 ) diff = 6.28 - diff;
									if( diff <= -3.14 ) diff = -6.28 - diff;

									if( diff < 0.5f && diff > -0.5f)
									{
										actRound = round[i];
										testStatus = 0;
										triggerIndex = 1;
										if( actRound.type == 0 )
										{
											acceleration = 1;
											acceleration120 = 1;
										}
										else
										{
											acceleration = 0;
											acceleration120 = 0;
										}
									}
								}
							}
						}
					}
				}
				else
				{
				}
			}
		}
	}

	public void setGroup(int gid)
	{
		if(actGroup != gid)
		{
			losd.hideGroup(actGroup);
			actGroup = gid;
			losd.showGroup(actGroup);
		}
	}

	public void animate()
	{
		super.animate();

		if( actRound )
		{
			float speedSquare = player.car.getSpeedSquare();
			String str;

			if( player.car.maxTestTrackSpeedSq < speedSquare ) player.car.maxTestTrackSpeedSq = speedSquare;

			if( testStatus == 0 ) //initial state for all tests
			{
				setGroup(actRound.osdGroup); //RAXAT: v2.3.1, OSD groups instead of texts

				if(actGroup == accelGroup) str = "0-100KPH (0-62.1MPH)";
				else str = "Lap time";

				testStatus = 1;
			}
			if( testStatus == 1 )
			{
				if( speedSquare > 0.25 )
				{
					setGroup(timerGroup); //RAXAT: v2.3.1, OSD groups instead of texts

					testStartTime = System.simTime();
					testStatus = 2;
					Sound.changeMusicSet(Sound.MUSIC_SET_RACE);
				}
			}
			if( testStatus == 2 )
			{
				float time = System.simTime() - testStartTime;
				messageTextB.changeText(String.timeToString(time, String.TCF_NOHOURS) + "s");

				if( acceleration )
				{
					if( speedSquare >= MPH_60 )
					{
						clearMsgBoxes();
						showMsgBox("0-100 KPH (0-62.1 MPH): " + String.timeToString(time, String.TCF_NOHOURS) + "s", Track.MBOX_YELLOW, Track.MBOX_MID);
						str = "0-200KPH (0-124.2MPH)";

						acceleration = 0;
						if( player.car.bestTestTrackAcc > time || player.car.bestTestTrackAcc < 0.10 ) player.car.bestTestTrackAcc = time;
					}
				}

				if( acceleration120 )
				{
					if( speedSquare >= MPH_124 )
					{
						clearMsgBoxes();
						showMsgBox("0-200 KPH (0-124.2 MPH): " + String.timeToString(time, String.TCF_NOHOURS) + "s", Track.MBOX_YELLOW, Track.MBOX_MID);
						str = "1/4 mile";

						acceleration120 = 0;
						if( player.car.bestTestTrackAcc120 > time || player.car.bestTestTrackAcc120 < 0.10 ) player.car.bestTestTrackAcc120 = time;
					}
				}

				if( activeTrigger )
				{
					Trigger tr = actRound.trigger.elementAt( triggerIndex );
					if( activeTrigger == tr.trigger.id() )
					{
						if( triggerIndex == 1 ) // 402 m //
						{
							if( actRound.type == 0 )
							{
								clearMsgBoxes();
								showMsgBox("1/4 MILE TIME: " + String.timeToString(time, String.TCF_NOHOURS) + "s", Track.MBOX_YELLOW, Track.MBOX_MID);
								str = "1 mile";

								if( player.car.bestTestTrackTime2 > time || player.car.bestTestTrackTime2 < 0.10 )
								{
									player.car.bestTestTrackTime2_speedSq = speedSquare;
									player.car.bestTestTrackTime2 = time;
								}
							}
						}
						triggerIndex++;
						if( triggerIndex == actRound.trigger.size() )
						{
							if( actRound.type == 0 )
							{
								setGroup(defGroup);
								clearMsgBoxes();
								showMsgBox("1 MILE TIME: " + String.timeToString(time, String.TCF_NOHOURS) + "s", Track.MBOX_GREEN, Track.MBOX_MID);

								if( player.car.bestTestTrackTime0 > time || player.car.bestTestTrackTime0 < 0.10 )
								{
									player.car.bestTestTrackTime0_speedSq = speedSquare;
									player.car.bestTestTrackTime0 = time;
								}
							}
							else
							{
								setGroup(defGroup);
								showMsgBox("LAP TIME: " + String.timeToString(time, String.TCF_NOHOURS) + "s", Track.MBOX_GREEN, Track.MBOX_MID);

								if( player.car.bestTestTrackTime1 > time || player.car.bestTestTrackTime1 < 0.10 ) player.car.bestTestTrackTime1 = time;
							}
							actRound = null;
							Sound.changeMusicSet( Sound.MUSIC_SET_DRIVING );
						}
					}
				}
			}
			if(str) messageTextA.changeText(str);
		}
	}

}


