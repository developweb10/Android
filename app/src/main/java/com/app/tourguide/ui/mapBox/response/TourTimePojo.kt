package com.app.tourguide.ui.mapBox.response

data class TourTimePojo(val packageId: String, val endTourTime: Long, val tourTokenId: String, val statusActivation: Boolean, val statusReturn: Boolean, val statusInactivation: Boolean,val doubleTourStatus:Boolean)