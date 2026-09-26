package dev.ajvanegasv.kontio.presentation.dashboard.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * Iconos vectoriales nativos para el Dashboard de Kontio correspondientes a los Material Symbols del HTML.
 */
object DashboardIcons {

    private inline fun buildIcon(name: String, crossinline block: PathBuilder.() -> Unit): ImageVector {
        return ImageVector.Builder(
            name = name,
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = SolidColor(Color.White),
                pathBuilder = { block() }
            )
        }.build()
    }

    val ArrowUpward: ImageVector by lazy {
        buildIcon("ArrowUpward") {
            moveTo(4f, 12f)
            lineTo(5.41f, 13.41f)
            lineTo(11f, 7.83f)
            verticalLineTo(20f)
            horizontalLineTo(13f)
            verticalLineTo(7.83f)
            lineTo(18.58f, 13.42f)
            lineTo(20f, 12f)
            lineTo(12f, 4f)
            lineTo(4f, 12f)
            close()
        }
    }

    val ArrowDownward: ImageVector by lazy {
        buildIcon("ArrowDownward") {
            moveTo(20f, 12f)
            lineTo(18.59f, 10.59f)
            lineTo(13f, 16.17f)
            verticalLineTo(4f)
            horizontalLineTo(11f)
            verticalLineTo(16.17f)
            lineTo(5.42f, 10.58f)
            lineTo(4f, 12f)
            lineTo(12f, 20f)
            lineTo(20f, 12f)
            close()
        }
    }

    val LiveTv: ImageVector by lazy {
        buildIcon("LiveTv") {
            moveTo(21f, 6f)
            horizontalLineTo(13.41f)
            lineTo(16.7f, 2.71f)
            lineTo(16f, 2f)
            lineTo(12f, 6f)
            lineTo(8f, 2f)
            lineTo(7.29f, 2.71f)
            lineTo(10.59f, 6f)
            horizontalLineTo(3f)
            curveTo(1.9f, 6f, 1f, 6.89f, 1f, 8f)
            verticalLineTo(20f)
            curveTo(1f, 21.1f, 1.9f, 22f, 3f, 22f)
            horizontalLineTo(21f)
            curveTo(22.1f, 22f, 23f, 21.1f, 23f, 20f)
            verticalLineTo(8f)
            curveTo(23f, 6.89f, 22.1f, 6f, 21f, 6f)
            close()
            moveTo(21f, 20f)
            horizontalLineTo(3f)
            verticalLineTo(8f)
            horizontalLineTo(21f)
            verticalLineTo(20f)
            close()
        }
    }

    val ShoppingCart: ImageVector by lazy {
        buildIcon("ShoppingCart") {
            moveTo(7f, 18f)
            curveTo(5.9f, 18f, 5.01f, 18.9f, 5.01f, 20f)
            curveTo(5.01f, 21.1f, 5.9f, 22f, 7f, 22f)
            curveTo(8.1f, 22f, 9f, 21.1f, 9f, 20f)
            curveTo(9f, 18.9f, 8.1f, 18f, 7f, 18f)
            close()
            moveTo(1f, 2f)
            verticalLineTo(4f)
            horizontalLineTo(3f)
            lineTo(6.6f, 11.59f)
            lineTo(5.25f, 14.04f)
            curveTo(5.09f, 14.32f, 5f, 14.65f, 5f, 15f)
            curveTo(5f, 16.1f, 5.9f, 17f, 7f, 17f)
            horizontalLineTo(19f)
            verticalLineTo(15f)
            horizontalLineTo(7.42f)
            curveTo(7.28f, 15f, 7.17f, 14.89f, 7.17f, 14.75f)
            lineTo(7.2f, 14.63f)
            lineTo(8.1f, 13f)
            horizontalLineTo(15.55f)
            curveTo(16.3f, 13f, 16.96f, 12.59f, 17.3f, 11.97f)
            lineTo(20.88f, 5.48f)
            curveTo(20.96f, 5.34f, 21f, 5.17f, 21f, 5f)
            curveTo(21f, 4.45f, 20.55f, 4f, 20f, 4f)
            horizontalLineTo(5.21f)
            lineTo(4.27f, 2f)
            horizontalLineTo(1f)
            close()
            moveTo(17f, 18f)
            curveTo(15.9f, 18f, 15.01f, 18.9f, 15.01f, 20f)
            curveTo(15.01f, 21.1f, 15.9f, 22f, 17f, 22f)
            curveTo(18.1f, 22f, 19f, 21.1f, 19f, 20f)
            curveTo(19f, 18.9f, 18.1f, 18f, 17f, 18f)
            close()
        }
    }

    val AccountBalanceWallet: ImageVector by lazy {
        buildIcon("AccountBalanceWallet") {
            moveTo(21f, 18f)
            verticalLineTo(19f)
            curveTo(21f, 20.1f, 20.1f, 21f, 19f, 21f)
            horizontalLineTo(5f)
            curveTo(3.89f, 21f, 3f, 20.1f, 3f, 19f)
            verticalLineTo(5f)
            curveTo(3f, 3.9f, 3.89f, 3f, 5f, 3f)
            horizontalLineTo(19f)
            curveTo(20.1f, 3f, 21f, 3.9f, 21f, 5f)
            verticalLineTo(6f)
            horizontalLineTo(12f)
            curveTo(10.89f, 6f, 10f, 6.9f, 10f, 8f)
            verticalLineTo(16f)
            curveTo(10f, 17.1f, 10.89f, 18f, 12f, 18f)
            horizontalLineTo(21f)
            close()
            moveTo(12f, 16f)
            horizontalLineTo(22f)
            verticalLineTo(8f)
            horizontalLineTo(12f)
            verticalLineTo(16f)
            close()
            moveTo(16f, 13.5f)
            curveTo(15.17f, 13.5f, 14.5f, 12.83f, 14.5f, 12f)
            curveTo(14.5f, 11.17f, 15.17f, 10.5f, 16f, 10.5f)
            curveTo(16.83f, 10.5f, 17.5f, 11.17f, 17.5f, 12f)
            curveTo(17.5f, 12.83f, 16.83f, 13.5f, 16f, 13.5f)
            close()
        }
    }

    val LocalCafe: ImageVector by lazy {
        buildIcon("LocalCafe") {
            moveTo(20f, 3f)
            horizontalLineTo(4f)
            verticalLineTo(13f)
            curveTo(4f, 15.21f, 5.79f, 17f, 8f, 17f)
            horizontalLineTo(14f)
            curveTo(16.21f, 17f, 18f, 15.21f, 18f, 13f)
            verticalLineTo(10f)
            horizontalLineTo(20f)
            curveTo(21.1f, 10f, 22f, 9.1f, 22f, 8f)
            verticalLineTo(5f)
            curveTo(22f, 3.9f, 21.1f, 3f, 20f, 3f)
            close()
            moveTo(20f, 8f)
            horizontalLineTo(18f)
            verticalLineTo(5f)
            horizontalLineTo(20f)
            verticalLineTo(8f)
            close()
            moveTo(2f, 21f)
            horizontalLineTo(20f)
            verticalLineTo(19f)
            horizontalLineTo(2f)
            verticalLineTo(21f)
            close()
        }
    }

    val Home: ImageVector by lazy {
        buildIcon("Home") {
            moveTo(10f, 20f)
            verticalLineTo(14f)
            horizontalLineTo(14f)
            verticalLineTo(20f)
            horizontalLineTo(19f)
            verticalLineTo(12f)
            horizontalLineTo(22f)
            lineTo(12f, 3f)
            lineTo(2f, 12f)
            horizontalLineTo(5f)
            verticalLineTo(20f)
            close()
        }
    }

    val Leaderboard: ImageVector by lazy {
        buildIcon("Leaderboard") {
            moveTo(7.5f, 21f)
            horizontalLineTo(2f)
            verticalLineTo(9f)
            horizontalLineTo(7.5f)
            verticalLineTo(21f)
            close()
            moveTo(14.75f, 3f)
            horizontalLineTo(9.25f)
            verticalLineTo(21f)
            horizontalLineTo(14.75f)
            verticalLineTo(3f)
            close()
            moveTo(22f, 11f)
            horizontalLineTo(16.5f)
            verticalLineTo(21f)
            horizontalLineTo(22f)
            verticalLineTo(11f)
            close()
        }
    }

    val AddCircle: ImageVector by lazy {
        buildIcon("AddCircle") {
            moveTo(12f, 2f)
            curveTo(6.48f, 2f, 2f, 6.48f, 2f, 12f)
            curveTo(2f, 17.52f, 6.48f, 22f, 12f, 22f)
            curveTo(17.52f, 22f, 22f, 17.52f, 22f, 12f)
            curveTo(22f, 6.48f, 17.52f, 2f, 12f, 2f)
            close()
            moveTo(17f, 13f)
            horizontalLineTo(13f)
            verticalLineTo(17f)
            horizontalLineTo(11f)
            verticalLineTo(13f)
            horizontalLineTo(7f)
            verticalLineTo(11f)
            horizontalLineTo(11f)
            verticalLineTo(7f)
            horizontalLineTo(13f)
            verticalLineTo(11f)
            horizontalLineTo(17f)
            verticalLineTo(13f)
            close()
        }
    }

    val CreditCard: ImageVector by lazy {
        buildIcon("CreditCard") {
            moveTo(20f, 4f)
            horizontalLineTo(4f)
            curveTo(2.89f, 4f, 2.01f, 4.89f, 2.01f, 6f)
            lineTo(2f, 18f)
            curveTo(2f, 19.11f, 2.89f, 20f, 4f, 20f)
            horizontalLineTo(20f)
            curveTo(21.11f, 20f, 22f, 19.11f, 22f, 18f)
            verticalLineTo(6f)
            curveTo(22f, 4.89f, 21.11f, 4f, 20f, 4f)
            close()
            moveTo(20f, 18f)
            horizontalLineTo(4f)
            verticalLineTo(12f)
            horizontalLineTo(20f)
            verticalLineTo(18f)
            close()
            moveTo(20f, 8f)
            horizontalLineTo(4f)
            verticalLineTo(6f)
            horizontalLineTo(20f)
            verticalLineTo(8f)
            close()
        }
    }

    val Person: ImageVector by lazy {
        buildIcon("Person") {
            moveTo(12f, 12f)
            curveTo(14.21f, 12f, 16f, 10.21f, 16f, 8f)
            curveTo(16f, 5.79f, 14.21f, 4f, 12f, 4f)
            curveTo(9.79f, 4f, 8f, 5.79f, 8f, 8f)
            curveTo(8f, 10.21f, 9.79f, 12f, 12f, 12f)
            close()
            moveTo(12f, 14f)
            curveTo(9.33f, 14f, 4f, 15.34f, 4f, 18f)
            verticalLineTo(20f)
            horizontalLineTo(20f)
            verticalLineTo(18f)
            curveTo(20f, 15.34f, 14.67f, 14f, 12f, 14f)
            close()
        }
    }

    val Notifications: ImageVector by lazy {
        buildIcon("Notifications") {
            moveTo(12f, 22f)
            curveTo(13.1f, 22f, 14f, 21.1f, 14f, 20f)
            horizontalLineTo(10f)
            curveTo(10f, 21.1f, 10.9f, 22f, 12f, 22f)
            close()
            moveTo(18f, 16f)
            verticalLineTo(11f)
            curveTo(18f, 7.93f, 16.36f, 5.36f, 13.5f, 4.68f)
            verticalLineTo(4f)
            curveTo(13.5f, 3.17f, 12.83f, 2.5f, 12f, 2.5f)
            curveTo(11.17f, 2.5f, 10.5f, 3.17f, 10.5f, 4f)
            verticalLineTo(4.68f)
            curveTo(7.63f, 5.36f, 6f, 7.92f, 6f, 11f)
            verticalLineTo(16f)
            lineTo(4f, 18f)
            verticalLineTo(19f)
            horizontalLineTo(20f)
            verticalLineTo(18f)
            lineTo(18f, 16f)
            close()
        }
    }

    val Add: ImageVector by lazy {
        buildIcon("Add") {
            moveTo(19f, 13f)
            horizontalLineTo(13f)
            verticalLineTo(19f)
            horizontalLineTo(11f)
            verticalLineTo(13f)
            horizontalLineTo(5f)
            verticalLineTo(11f)
            horizontalLineTo(11f)
            verticalLineTo(5f)
            horizontalLineTo(13f)
            verticalLineTo(11f)
            horizontalLineTo(19f)
            verticalLineTo(13f)
            close()
        }
    }
}
