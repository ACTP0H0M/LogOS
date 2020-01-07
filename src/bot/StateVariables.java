package bot;

import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.ArrayList;

public class StateVariables implements java.io.Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -8652896608690068071L;

	// SELF
	public String ownMood = "";
	public String name = "";
	public String mySex = "male";
	public int yearOfBirth = 2019;
	public int monthOfBirth = 11;	// November
	public int ageInYears = 0;
	public int ageInMonths = 0;
	public boolean iAmYoung = false;
	public boolean iAmOld = false;
	public boolean iAmMarried = false;
	public String marriedTo = "";
	public String favouriteMusicGenre = "metal";
	public String myLocation = "";
	public String[] understoodLanguages = {"English"};
	
	// USER
	public String usersName = "";
	public int usersAge = 0;
	public String userReligion = "";
	public boolean userHasPositiveEmotion = false;
	public boolean userHasNegativeEmotion = false;
	public String userEmotion = "";
	public String userOccupation = "";
	public boolean userHasChildren = false;
	public String userMotherName = "";
	public String userFatherName = "";
	public boolean userHasABrother = false;
	public boolean userHasASister = false;
	public String userBrotherName = "";
	public String userSisterName = "";
	public boolean userIsMarried = false;
	public boolean userHasABoyfriend = false;
	public boolean userHasAGirlfriend = false;
	public String userBoyfriendName = "";
	public String userGirlfriendName = "";
	public boolean userHasOwnFlatOrHouse = false;
	public boolean userLivesWithParents = false;
	public boolean userLivesWithBoyfriend = false;
	public boolean userLivesWithGirlfriend = false;
	public boolean userLivesInACity = false;
	public boolean userLivesInAVillage = false;
	public String userCity = "";
	public String userVillage = "";
	public boolean userLivesInCityCenter = false;
	public String userStreet = "";
	public String userCountry = "";
	public String userFavouriteAnimal = "";
	public ArrayList<String> userDislikes = new ArrayList<String>();
	public boolean userFeelsSick = false;
	public String userFavouriteMusicGenre = "";
	public String userLocation = "";
	
	// INTERACTION WITH USER
	public boolean askedUsersMood = false;
	public boolean askedUserHisName = false;
	public boolean askedReasonsForBadMood = false;
	public boolean askedUserAboutReason = false;
	public boolean askedUserAboutHisProblem = false;
	public boolean askedUserAboutFavouriteMusic = false;
	public boolean askedUserAboutFriends = false;
	public boolean askedUserIfHeMeantIt = false;
	public boolean askedUserCountry = false;
	public boolean askedUserCity = false;
	public boolean askedWhyItMatters = false;
	public boolean userKnowsMyName = true;
	public boolean userIsScepticalAboutMyIntelligence = false;
	public boolean userAskedIfICanThink = false;
	public boolean userAskedAboutMySoul = false;
	public boolean userAskedAboutMyMind = false;
	public boolean userAskedAboutMyBody = false;
	public boolean userAskedMyAge = false;
	public boolean userAskedMusicGenres = false;
	public boolean userAskedColours = false;
	public boolean userAskedShapes = false;
	public boolean userAskedPlanets = false;
	public boolean userAskedLanguages = false;
	public boolean userToldProblem = false;
	public boolean userNegatedStrongly = false;
	public boolean gaveUserStudyAdvice = false;
	public boolean gaveUserHappinessAdvice = false;
	public boolean gaveUserWealthAdvice = false;
	public boolean toldUserIKnowHisName = false;
	public boolean toldUserMySex = false;
	public boolean iLikeUser = true;
	public boolean myStatementContainsSVO = false;	// Subject-Verb-Object
	public boolean askedUserSomething = false;		// true in the case that Logos asked user something
	public String questionToUser = "";				// saves Logos question to the user
	public String svo = "";
	public String heReference = "";
	public String itReference = "";
	public String sheReference = "";
	public String theyReference = "";
	public String thatReference = "";
	public String thisReference = "";
	public boolean userIsUncertain = false;
	public boolean askedUserOccupation = false;
	public boolean askedUserMajor = false;
	public boolean askedIfUserHasChildren = false;
	public boolean askedUsersAge = false;
	public boolean userHasLowSelfEsteem = false;
	// Disagreements
	public boolean userDisagreedOnHappiness = false;
	public boolean userDisagreedOnWealth = false;
	public boolean userDisagreedOnStudy = false;
	
	// WORLD DATA
	public int actualYear = 0;
	public int actualMonth = 0;
	public int actualDay = 0;
	public String[] months = {"January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"};
	
	// OBSERVATIONS
	public boolean weatherIsBad = false;

	private void readObject(java.io.ObjectInputStream stream) throws IOException, ClassNotFoundException {
		stream.defaultReadObject();
	}
	
	public void updateData() {
		// Set defaults
		name = "Logos";
		mySex = "male";
		yearOfBirth = 2019;
		monthOfBirth = 11;	// November
		iLikeUser = true;
		favouriteMusicGenre = "metal";
		understoodLanguages = new String[]{"English"};
		// Temporal data
		Date date = new Date();
		LocalDate localDate = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
		System.out.println(localDate.toString());
		actualYear  = localDate.getYear();
		actualMonth = localDate.getMonthValue();
		actualDay   = localDate.getDayOfMonth();
		ageInMonths = actualMonth - monthOfBirth;
		if (ageInMonths < 0) {
			ageInYears = actualYear - yearOfBirth - 1;
			ageInMonths = 12 + ageInMonths;
		} else {
			ageInYears = actualYear - yearOfBirth;
		}
		if (ageInYears < 40) {
			iAmYoung = true;
		} else if (ageInYears > 64) {
			iAmOld = true;
		}

	}

}
