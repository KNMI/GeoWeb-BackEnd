2019-11-25
* (FIX) GW-229: For cancelled TAFs, the exported TAC had the incorrect validity start - this should be updated when cancelling to the moment of issuing the cancelled TAF: TAF EHRD 261025Z 2606/2712 20020KT CAVOK= -> TAF AMD EHRD 261025Z 2610/2712 CNL=. This was done on screen in the FE, but the exported TAC was wrong. This has been fixed so the exported TAC now has the correct validity start time

2019-11-25
* (FIX) GW-193: TAF-cancelled: The original TAF of a cancelled TAF was still being passed from the backend for the TAF list which therefore showed up in the list. This has been fixed to ensure the original TAF is not passed from the backend. 

21-11-2019
- GW-222: fixed NPE with empty phenomenon or empty geometry in toTAC() for AIRMET/SIGMET
- GW-223: For the TAC message, visibility for SFC_VIS is capped at 9999 (TAC does not allow five digits).
- GW-166: In the dropdown list for setting the visibility cause, "Mist" is now changed to "Mist (BR)"
