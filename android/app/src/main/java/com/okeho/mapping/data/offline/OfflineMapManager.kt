package com.okeho.mapping.data.offline

import android.content.Context
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.views.MapView
import java.io.File

object OfflineMapManager {
    private const val OKEHO_LAT = 8.03
    private const val OKEHO_LNG = 3.70
    private const val ZOOM_MIN = 12
    private const val ZOOM_MAX = 17

    fun initialize(context: Context) {
        Configuration.getInstance().apply {
            load(context, context.getSharedPreferences("osmdroid", Context.MODE_PRIVATE))
            osmdroidBasePath = File(context.filesDir, "osmdroid")
            osmdroidTileCache = File(context.cacheDir, "tiles")
        }
    }

    fun configureMapForOffline(mapView: MapView) {
        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.setMultiTouchControls(true)
        mapView.controller.setZoom(15.0)
        mapView.controller.setCenter(org.osmdroid.util.GeoPoint(OKEHO_LAT, OKEHO_LNG))
    }
}
