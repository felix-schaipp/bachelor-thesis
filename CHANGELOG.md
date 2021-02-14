0.71 ALPHA (10-07-2019)
---------------------
- rewrote communication class
- rxtx library instead of javax.comm library
- code cleanup

0.7 ALPHA (09-07-2019)
---------------------
- implemented data storage with dummy data
- data storage now working with json instead of .txt
- added Changelog to GIT
- added Licences to GIT
- updated README
- new Button for saving data to the db file

0.6 ALPHA (05-07-2019)
----------------------
- small logic changes
- new logic to when moving the table and finding the next printing mark will be executed (wihtout threads)
- new methods for communicating with the table
- rewrote the table communication classes
- rewrote getPrintingMarks() with new file input and searching for a dedicated color
- cleanup

0.55 ALPHA (04-07-2019)
----------------------
- new Buttons and Labels in the control panel
- adjusted new thread system for waiting till the next printing mark is found

0.54 ALPHA (29-06-2019)
----------------------
- adjusted finding the correct quadrants for the marks and reordering them
- new thread system for waiting till the next printing mark is found
- get width and height from svg root
- new function for finding the right quadrants

0.53 ALPHA (23-06-2019)
----------------------
- added a function to calculate the offset of the detected circle
- display the center of the camera frame
- show if the circle was detected

0.52 ALPHA (20.06-2019)
----------------------
- get the center position of the circle and show in GUI
- Util class
- code cleanup

0.51 ALPHA (19.06-2019)
----------------------
- new flexible function for finding the printing marks with possible fill option
- new GUI elements for the frontend
- move function on key press and mouse click

0.5 ALPHA (18-06-2019)
----------------------
- added sliders to change the hough circle algorithm values
- added complete circle detection
- added certainty princible for finding a circle

0.4 ALPHA (20-03-2019)
----------------------
- rewrote the Hough Circle functions to fit our needs

0.3 ALPHA (27-02-2019)
----------------------
- printing code results to frontend
- new GUI elements for controlling the camera

0.2 ALPHA (11-02-2019)
----------------------
- file input of svg
- integrating xpath library, searching for printing marks in svg by creating the search queries for xpath

0.1 ALPHA (10-02-2019)
----------------------
- initial application
- setting up the project
