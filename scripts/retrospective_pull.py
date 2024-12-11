import requests
import logging
from config import REST_SOURCES_AUTH_BACKEND_URL, PROJECT_ID, SOURCE_TYPE, TOKEN, START_DATE, END_DATE, PULL_PERIOD_IN_WEEKS, INCLUDE_SUBJECTS, EXCLUDE_SUBJECTS, ENABLE_DEBUG
from datetime import datetime
import datetime as dt
from dateutil import parser

logger = logging.getLogger(__name__)
logging.basicConfig(format='%(asctime)s - %(message)s', datefmt='%d-%b-%y %H:%M:%S', level=logging.INFO)

if ENABLE_DEBUG:
    logger.setLevel(logging.DEBUG)

if __name__ == "__main__":

    # Ask user input for dry run option
    dry_run = input("Dry run? (y/n): ")
    if dry_run.lower() == "y":
        logger.info("Dry run enabled")
    elif dry_run.lower() == "n":
        logger.info("Running retrospective pull reset script")
    else:
        logger.error("Invalid input. Please enter 'y' or 'n'")
        exit()

    now = datetime.now(dt.timezone.utc)

    # make request to get all the subjects from the REST_SOURCES_AUTH_BACKEND_URL
    response = requests.get(REST_SOURCES_AUTH_BACKEND_URL + f"/users?project-id={PROJECT_ID}&source-type={SOURCE_TYPE}&authorized=true", headers={"Authorization": "Bearer " + TOKEN})
    
    if response.status_code != 200:
        logger.error(f"{response.json().get('error', 'Error')}: {response.json().get('error_description', 'Error fetching subjects')}")
        exit()
    
    subjects = response.json()['users']

    logger.debug(f"Type: {type(subjects)} ,Subjects: {subjects}")

    if len(subjects) == 0:
        logger.error("No subjects found")
        exit()
    
    confirm = input(f"Are you sure you want to reset {len(subjects)} subjects? (y/n): ")
    if confirm.lower() != "y":
        logger.info("Exiting")
        exit()

    # for each subject, make a request to the REST_SOURCES_AUTH_BACKEND_URL to update the user's start and end date and reset the user
    for subject in subjects:

        # check if the subject is in the include list
        if len(INCLUDE_SUBJECTS) > 0 and subject["id"] not in INCLUDE_SUBJECTS:
            continue

        # check if the subject is in the exclude list
        if len(EXCLUDE_SUBJECTS) > 0 and subject["id"] in EXCLUDE_SUBJECTS:
            continue

        if START_DATE and END_DATE:
            start_date = datetime.strptime(START_DATE, "%Y-%m-%dT%H%M%SZ")
            end_date = datetime.strptime(END_DATE, "%Y-%m-%dT%H%M%SZ")
        elif PULL_PERIOD_IN_WEEKS:
            start_date = now - dt.timedelta(weeks=PULL_PERIOD_IN_WEEKS)
            end_date = now
            
        else:
            raise Exception("Either PULL_PERIOD_IN_WEEKS or START_DATE + END_DATE must be specified")

        subject["startDate"] = start_date.timestamp()
        subject["endDate"] = end_date.timestamp()

        if dry_run.lower() == "n":
            response = requests.post(REST_SOURCES_AUTH_BACKEND_URL + f"/users/{subject['id']}/reset", headers={"Authorization": "Bearer " + TOKEN}, json=subject)
            
            logger.debug(f"Response: {response.json()}")

            # Write a test case to check and confirm the user has been updated as expected
            if response.status_code == 200:
                start_date_res = parser.parse(response.json()['startDate'])
                end_date_res = parser.parse(response.json()['endDate'])
                logger.debug(f"start date: {start_date.timestamp()}, end date: {end_date.timestamp()}")
                logger.debug(f"start date res: {start_date_res.timestamp()}, end date res: {end_date_res.timestamp()}")

                assert start_date_res.timestamp() == start_date.timestamp()
                assert end_date_res.timestamp() == end_date.timestamp()

                logger.info(f"User {subject['userId']} reset successfully")
            else:
                if response.json().get('error') == "user_not_found":
                    logger.error(f"Error: {response.json().get('error_description', f"User {subject['userId']} not found in MP. Please check the user id")}")
        elif dry_run.lower() == "y":
            logger.info(f"DRY RUN: {subject}")

    logger.info("All reset updated successfully")
    exit()
