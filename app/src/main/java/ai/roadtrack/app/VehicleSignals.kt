package ai.roadtrack.app

import ai.pleos.playground.vehicle.Vehicle
import ai.pleos.playground.vehicle.api.model.area.SeatArea
import ai.pleos.playground.vehicle.api.model.area.WindowArea
import ai.pleos.playground.vehicle.constant.area.SeatValue
import ai.pleos.playground.vehicle.constant.area.WindowValue
import ai.pleos.playground.vehicle.constant.control.LightState
import ai.pleos.playground.vehicle.constant.control.VehicleGear
import ai.pleos.playground.vehicle.constant.control.WiperState
import ai.pleos.playground.vehicle.listener.GearSelectionListener
import ai.pleos.playground.vehicle.listener.LightStateListener
import ai.pleos.playground.vehicle.listener.SeatOccupancyListener
import ai.pleos.playground.vehicle.listener.WiperStateListener
import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

/** Gear, wiper, low beam and passenger seat from the Pleos Vehicle SDK, merged into one [CarSignals] flow. */
class VehicleSignals(context: Context) {
    private val vehicle = Vehicle(context)
    private val _state = MutableStateFlow(CarSignals())
    val state: StateFlow<CarSignals> = _state

    private val windshield = WindowArea(WindowValue.FRONT_WINDSHIELD)
    private val passengerSeat = SeatArea(SeatValue.ROW_1_RIGHT)
    private val driverSeat = SeatArea(SeatValue.ROW_1_LEFT)
    private val fail: (Exception) -> Unit = { Log.w(TAG, "vehicle call failed", it) }

    private val gear = object : GearSelectionListener {
        override fun onSelected(p0: VehicleGear?) = onGear(p0)
        override fun onFailed(p0: Exception) = fail(p0)
    }
    private val wiper = object : WiperStateListener {
        override fun onWiperStateUpdated(p0: WiperState?) = onWiper(p0)
        override fun onFailed(p0: Exception) = fail(p0)
    }
    private val lowBeam = object : LightStateListener {
        override fun onLightStateUpdated(p0: LightState?) = onLowBeam(p0)
        override fun onFailed(p0: Exception) = fail(p0)
    }
    private val seat = object : SeatOccupancyListener {
        override fun onSeatOccupancyUpdated(p0: Boolean?) = onSeat(p0)
        override fun onFailed(p0: Exception) = fail(p0)
    }

    fun start() {
        // Off a Pleos AVD the service is missing; keep the defaults and let the UI run.
        runCatching {
            vehicle.initialize()
            vehicle.getGear().run { getCurrentGearSelectionState(::onGear, fail); registerGearSelection(gear) }
            vehicle.getWiper().run { getCurrentWiperState(windshield, ::onWiper, fail); registerWiperState(windshield, wiper) }
            vehicle.getLight().run { getCurrentLowBeamLightState(::onLowBeam, fail); registerLowBeamLightState(lowBeam) }
            vehicle.getSeat().run { getCurrentSeatOccupancy(passengerSeat, ::onSeat, fail); registerSeatOccupancy(passengerSeat, seat) }
        }.onFailure { Log.w(TAG, "Vehicle SDK unavailable", it) }
    }

    fun stop() {
        runCatching {
            vehicle.getGear().unregisterGearSelection(gear)
            vehicle.getWiper().unregisterWiperState(windshield, wiper)
            vehicle.getLight().unregisterLowBeamLightState(lowBeam)
            vehicle.getSeat().unregisterSeatOccupancy(passengerSeat, seat)
            vehicle.release()
        }
    }

    /** Ambient light follows the mood. Needs CONTROL_CAR_LIGHTS, which App Market must approve. */
    fun setAmbient(argb: Int) {
        runCatching { vehicle.getLight().setAmbientLightBrightnessColor(driverSeat, argb, { _, _ -> }, fail) }
            .onFailure { Log.w(TAG, "ambient light unavailable", it) }
    }

    private fun onGear(g: VehicleGear?) = _state.update { it.copy(parked = g == VehicleGear.GEAR_PARK) }
    private fun onWiper(w: WiperState?) = _state.update { it.copy(raining = w == WiperState.ON) }
    private fun onLowBeam(l: LightState?) = _state.update { it.copy(night = l == LightState.ON) }
    private fun onSeat(occupied: Boolean?) = _state.update { it.copy(passenger = occupied == true) }

    companion object { private const val TAG = "VehicleSignals" }
}
