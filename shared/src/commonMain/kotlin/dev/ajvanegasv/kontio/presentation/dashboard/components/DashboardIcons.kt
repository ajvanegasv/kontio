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

    val Delete: ImageVector by lazy {
        buildIcon("Delete") {
            moveTo(6f, 19f)
            curveTo(6f, 20.1f, 6.9f, 21f, 8f, 21f)
            horizontalLineTo(16f)
            curveTo(17.1f, 21f, 18f, 20.1f, 18f, 19f)
            verticalLineTo(7f)
            horizontalLineTo(6f)
            verticalLineTo(19f)
            close()
            moveTo(19f, 4f)
            horizontalLineTo(15.5f)
            lineTo(14.5f, 3f)
            horizontalLineTo(9.5f)
            lineTo(8.5f, 4f)
            horizontalLineTo(5f)
            verticalLineTo(6f)
            horizontalLineTo(19f)
            verticalLineTo(4f)
            close()
        }
    }

    val ArrowBack: ImageVector by lazy {
        buildIcon("ArrowBack") {
            moveTo(20f, 11f)
            horizontalLineTo(7.83f)
            lineTo(13.42f, 5.41f)
            lineTo(12f, 4f)
            lineTo(4f, 12f)
            lineTo(12f, 20f)
            lineTo(13.41f, 18.59f)
            lineTo(7.83f, 13f)
            horizontalLineTo(20f)
            verticalLineTo(11f)
            close()
        }
    }

    val Search: ImageVector by lazy {
        buildIcon("Search") {
            moveTo(15.5f, 14f)
            horizontalLineTo(14.71f)
            lineTo(14.43f, 13.73f)
            curveTo(15.41f, 12.59f, 16f, 11.11f, 16f, 9.5f)
            curveTo(16f, 5.91f, 13.09f, 3f, 9.5f, 3f)
            curveTo(5.91f, 3f, 3f, 5.91f, 3f, 9.5f)
            curveTo(3f, 13.09f, 5.91f, 16f, 9.5f, 16f)
            curveTo(11.11f, 16f, 12.59f, 15.41f, 13.73f, 14.43f)
            lineTo(14f, 14.71f)
            verticalLineTo(15.5f)
            lineTo(19f, 20.49f)
            lineTo(20.49f, 19f)
            lineTo(15.5f, 14f)
            close()
            moveTo(9.5f, 14f)
            curveTo(7.01f, 14f, 5f, 11.99f, 5f, 9.5f)
            curveTo(5f, 7.01f, 7.01f, 5f, 9.5f, 5f)
            curveTo(11.99f, 5f, 14f, 7.01f, 14f, 9.5f)
            curveTo(14f, 11.99f, 11.99f, 14f, 9.5f, 14f)
            close()
        }
    }

    val Settings: ImageVector by lazy {
        buildIcon("Settings") {
            moveTo(19.14f, 12.94f)
            curveTo(19.18f, 12.64f, 19.2f, 12.33f, 19.2f, 12f)
            curveTo(19.2f, 11.68f, 19.18f, 11.36f, 19.13f, 11.06f)
            lineTo(21.27f, 9.39f)
            curveTo(21.46f, 9.24f, 21.51f, 8.97f, 21.39f, 8.76f)
            lineTo(19.39f, 5.3f)
            curveTo(19.27f, 5.09f, 19.01f, 5f, 18.79f, 5.09f)
            lineTo(16.27f, 6.1f)
            curveTo(15.75f, 5.7f, 15.18f, 5.38f, 14.56f, 5.14f)
            lineTo(14.18f, 2.45f)
            curveTo(14.14f, 2.2f, 13.93f, 2f, 13.68f, 2f)
            horizontalLineTo(9.68f)
            curveTo(9.43f, 2f, 9.22f, 2.2f, 9.18f, 2.45f)
            lineTo(8.8f, 5.14f)
            curveTo(8.18f, 5.38f, 7.61f, 5.7f, 7.09f, 6.1f)
            lineTo(4.57f, 5.09f)
            curveTo(4.35f, 5f, 4.09f, 5.09f, 3.97f, 5.3f)
            lineTo(1.97f, 8.76f)
            curveTo(1.85f, 8.97f, 1.9f, 9.24f, 2.09f, 9.39f)
            lineTo(4.23f, 11.06f)
            curveTo(4.18f, 11.37f, 4.16f, 11.69f, 4.16f, 12f)
            curveTo(4.16f, 12.31f, 4.18f, 12.63f, 4.23f, 12.94f)
            lineTo(2.09f, 14.61f)
            curveTo(1.9f, 14.76f, 1.85f, 15.03f, 1.97f, 15.24f)
            lineTo(3.97f, 18.7f)
            curveTo(4.09f, 18.91f, 4.35f, 19f, 4.57f, 18.91f)
            lineTo(7.09f, 17.9f)
            curveTo(7.61f, 18.3f, 8.18f, 18.62f, 8.8f, 18.86f)
            lineTo(9.18f, 21.55f)
            curveTo(9.22f, 21.8f, 9.43f, 22f, 9.68f, 22f)
            horizontalLineTo(13.68f)
            curveTo(13.93f, 22f, 14.14f, 21.8f, 14.18f, 21.55f)
            lineTo(14.56f, 18.86f)
            curveTo(15.18f, 18.62f, 15.75f, 18.3f, 16.27f, 17.9f)
            lineTo(18.79f, 18.91f)
            curveTo(19.01f, 19f, 19.27f, 18.91f, 19.39f, 18.7f)
            lineTo(21.39f, 15.24f)
            curveTo(21.51f, 15.03f, 21.46f, 14.76f, 21.27f, 14.61f)
            lineTo(19.14f, 12.94f)
            close()
            moveTo(12f, 15.5f)
            curveTo(10.07f, 15.5f, 8.5f, 13.93f, 8.5f, 12f)
            curveTo(8.5f, 10.07f, 10.07f, 8.5f, 12f, 8.5f)
            curveTo(13.93f, 8.5f, 15.5f, 10.07f, 15.5f, 12f)
            curveTo(15.5f, 13.93f, 13.93f, 15.5f, 12f, 15.5f)
            close()
        }
    }

    val MoreHoriz: ImageVector by lazy {
        buildIcon("MoreHoriz") {
            moveTo(6f, 10f)
            curveTo(4.9f, 10f, 4f, 10.9f, 4f, 12f)
            curveTo(4f, 13.1f, 4.9f, 14f, 6f, 14f)
            curveTo(7.1f, 14f, 8f, 13.1f, 8f, 12f)
            curveTo(8f, 10.9f, 7.1f, 10f, 6f, 10f)
            close()
            moveTo(12f, 10f)
            curveTo(10.9f, 10f, 10f, 10.9f, 10f, 12f)
            curveTo(10f, 13.1f, 10.9f, 14f, 12f, 14f)
            curveTo(13.1f, 14f, 14f, 13.1f, 14f, 12f)
            curveTo(14f, 10.9f, 13.1f, 10f, 12f, 10f)
            close()
            moveTo(18f, 10f)
            curveTo(16.9f, 10f, 16f, 10.9f, 16f, 12f)
            curveTo(16f, 13.1f, 16.9f, 14f, 18f, 14f)
            curveTo(19.1f, 14f, 20f, 13.1f, 20f, 12f)
            curveTo(20f, 10.9f, 19.1f, 10f, 18f, 10f)
            close()
        }
    }

    val Category: ImageVector by lazy {
        buildIcon("Category") {
            moveTo(12f, 2f)
            lineTo(6.5f, 11f)
            horizontalLineTo(17.5f)
            lineTo(12f, 2f)
            close()
            moveTo(17.5f, 13f)
            curveTo(15.01f, 13f, 13f, 15.01f, 13f, 17.5f)
            curveTo(13f, 19.99f, 15.01f, 22f, 17.5f, 22f)
            curveTo(19.99f, 22f, 22f, 19.99f, 22f, 17.5f)
            curveTo(22f, 15.01f, 19.99f, 13f, 17.5f, 13f)
            close()
            moveTo(3f, 13.5f)
            horizontalLineTo(11f)
            verticalLineTo(21.5f)
            horizontalLineTo(3f)
            verticalLineTo(13.5f)
            close()
        }
    }

    val ChevronRight: ImageVector by lazy {
        buildIcon("ChevronRight") {
            moveTo(10f, 6f)
            lineTo(8.59f, 7.41f)
            lineTo(13.17f, 12f)
            lineTo(8.59f, 16.59f)
            lineTo(10f, 18f)
            lineTo(16f, 12f)
            lineTo(10f, 6f)
            close()
        }
    }

    val Check: ImageVector by lazy {
        buildIcon("Check") {
            moveTo(9f, 16.17f)
            lineTo(4.83f, 12f)
            lineTo(3.41f, 13.41f)
            lineTo(9f, 19f)
            lineTo(21f, 7f)
            lineTo(19.59f, 5.59f)
            lineTo(9f, 16.17f)
            close()
        }
    }

    val Close: ImageVector by lazy {
        buildIcon("Close") {
            moveTo(19f, 6.41f)
            lineTo(17.59f, 5f)
            lineTo(12f, 10.59f)
            lineTo(6.41f, 5f)
            lineTo(5f, 6.41f)
            lineTo(10.59f, 12f)
            lineTo(5f, 17.59f)
            lineTo(6.41f, 19f)
            lineTo(12f, 13.41f)
            lineTo(17.59f, 19f)
            lineTo(19f, 17.59f)
            lineTo(13.41f, 12f)
            lineTo(19f, 6.41f)
            close()
        }
    }

    val DirectionsCar: ImageVector by lazy {
        buildIcon("DirectionsCar") {
            moveTo(18.92f, 6.01f)
            curveTo(18.72f, 5.42f, 18.16f, 5f, 17.5f, 5f)
            horizontalLineTo(6.5f)
            curveTo(5.84f, 5f, 5.28f, 5.42f, 5.08f, 6.01f)
            lineTo(3f, 12f)
            verticalLineTo(20f)
            curveTo(3f, 20.55f, 3.45f, 21f, 4f, 21f)
            horizontalLineTo(5f)
            curveTo(5.55f, 21f, 6f, 20.55f, 6f, 20f)
            verticalLineTo(19f)
            horizontalLineTo(18f)
            verticalLineTo(20f)
            curveTo(18f, 20.55f, 18.45f, 21f, 19f, 21f)
            horizontalLineTo(20f)
            curveTo(20.55f, 21f, 21f, 20.55f, 21f, 20f)
            verticalLineTo(12f)
            lineTo(18.92f, 6.01f)
            close()
            moveTo(6.85f, 7f)
            horizontalLineTo(17.14f)
            lineTo(18.22f, 10.11f)
            horizontalLineTo(5.77f)
            lineTo(6.85f, 7f)
            close()
            moveTo(7.5f, 16f)
            curveTo(6.67f, 16f, 6f, 15.33f, 6f, 14.5f)
            curveTo(6f, 13.67f, 6.67f, 13f, 7.5f, 13f)
            curveTo(8.33f, 13f, 9f, 13.67f, 9f, 14.5f)
            curveTo(9f, 15.33f, 8.33f, 16f, 7.5f, 16f)
            close()
            moveTo(16.5f, 16f)
            curveTo(15.67f, 16f, 15f, 15.33f, 15f, 14.5f)
            curveTo(15f, 13.67f, 15.67f, 13f, 16.5f, 13f)
            curveTo(17.33f, 13f, 18f, 13.67f, 18f, 14.5f)
            curveTo(18f, 15.33f, 17.33f, 16f, 16.5f, 16f)
            close()
        }
    }

    val MedicalServices: ImageVector by lazy {
        buildIcon("MedicalServices") {
            moveTo(20f, 6f)
            horizontalLineTo(16f)
            verticalLineTo(4f)
            curveTo(16f, 2.9f, 15.1f, 2f, 14f, 2f)
            horizontalLineTo(10f)
            curveTo(8.9f, 2f, 8f, 2.9f, 8f, 4f)
            verticalLineTo(6f)
            horizontalLineTo(4f)
            curveTo(2.9f, 6f, 2f, 6.9f, 2f, 8f)
            verticalLineTo(20f)
            curveTo(2f, 21.1f, 2.9f, 22f, 4f, 22f)
            horizontalLineTo(20f)
            curveTo(21.1f, 22f, 22f, 21.1f, 22f, 20f)
            verticalLineTo(8f)
            curveTo(22f, 6.9f, 21.1f, 6f, 20f, 6f)
            close()
            moveTo(10f, 4f)
            horizontalLineTo(14f)
            verticalLineTo(6f)
            horizontalLineTo(10f)
            verticalLineTo(4f)
            close()
            moveTo(20f, 20f)
            horizontalLineTo(4f)
            verticalLineTo(8f)
            horizontalLineTo(20f)
            verticalLineTo(20f)
            close()
            moveTo(11f, 17f)
            horizontalLineTo(13f)
            verticalLineTo(15f)
            horizontalLineTo(15f)
            verticalLineTo(13f)
            horizontalLineTo(13f)
            verticalLineTo(11f)
            horizontalLineTo(11f)
            verticalLineTo(13f)
            horizontalLineTo(9f)
            verticalLineTo(15f)
            horizontalLineTo(11f)
            verticalLineTo(17f)
            close()
        }
    }

    val School: ImageVector by lazy {
        buildIcon("School") {
            moveTo(5f, 13.18f)
            verticalLineTo(17.18f)
            lineTo(12f, 21f)
            lineTo(19f, 17.18f)
            verticalLineTo(13.18f)
            lineTo(12f, 17f)
            lineTo(5f, 13.18f)
            close()
            moveTo(12f, 3f)
            lineTo(1f, 9f)
            lineTo(12f, 15f)
            lineTo(21f, 10.09f)
            verticalLineTo(17f)
            horizontalLineTo(23f)
            verticalLineTo(9f)
            lineTo(12f, 3f)
            close()
        }
    }

    val Work: ImageVector by lazy {
        buildIcon("Work") {
            moveTo(20f, 6f)
            horizontalLineTo(16f)
            verticalLineTo(4f)
            curveTo(16f, 2.9f, 15.1f, 2f, 14f, 2f)
            horizontalLineTo(10f)
            curveTo(8.9f, 2f, 8f, 2.9f, 8f, 4f)
            verticalLineTo(6f)
            horizontalLineTo(4f)
            curveTo(2.9f, 6f, 2f, 6.9f, 2f, 8f)
            verticalLineTo(19f)
            curveTo(2f, 20.1f, 2.9f, 21f, 4f, 21f)
            horizontalLineTo(20f)
            curveTo(21.1f, 21f, 22f, 20.1f, 22f, 19f)
            verticalLineTo(8f)
            curveTo(22f, 6.9f, 21.1f, 6f, 20f, 6f)
            close()
            moveTo(10f, 4f)
            horizontalLineTo(14f)
            verticalLineTo(6f)
            horizontalLineTo(10f)
            verticalLineTo(4f)
            close()
            moveTo(20f, 19f)
            horizontalLineTo(4f)
            verticalLineTo(8f)
            horizontalLineTo(20f)
            verticalLineTo(19f)
            close()
        }
    }

    val CardGiftcard: ImageVector by lazy {
        buildIcon("CardGiftcard") {
            moveTo(20f, 6f)
            horizontalLineTo(17.82f)
            curveTo(17.93f, 5.69f, 18f, 5.35f, 18f, 5f)
            curveTo(18f, 3.34f, 16.66f, 2f, 15f, 2f)
            curveTo(13.95f, 2f, 13.04f, 2.54f, 12.5f, 3.35f)
            lineTo(12f, 4.02f)
            lineTo(11.5f, 3.34f)
            curveTo(10.96f, 2.54f, 10.05f, 2f, 9f, 2f)
            curveTo(7.34f, 2f, 6f, 3.34f, 6f, 5f)
            curveTo(6f, 5.35f, 6.07f, 5.69f, 6.18f, 6f)
            horizontalLineTo(4f)
            curveTo(2.89f, 6f, 2.01f, 6.89f, 2.01f, 8f)
            lineTo(2f, 19f)
            curveTo(2f, 20.11f, 2.89f, 21f, 4f, 21f)
            horizontalLineTo(20f)
            curveTo(21.11f, 21f, 22f, 20.11f, 22f, 19f)
            verticalLineTo(8f)
            curveTo(22f, 6.89f, 21.11f, 6f, 20f, 6f)
            close()
            moveTo(15f, 4f)
            curveTo(15.55f, 4f, 16f, 4.45f, 16f, 5f)
            curveTo(16f, 5.55f, 15.55f, 6f, 15f, 6f)
            horizontalLineTo(13f)
            verticalLineTo(5f)
            curveTo(13f, 4.45f, 13.45f, 4f, 14f, 4f)
            close()
            moveTo(9f, 4f)
            curveTo(9.55f, 4f, 10f, 4.45f, 10f, 5f)
            verticalLineTo(6f)
            horizontalLineTo(8f)
            curveTo(7.45f, 6f, 7f, 5.55f, 7f, 5f)
            curveTo(7f, 4.45f, 7.45f, 4f, 8f, 4f)
            close()
            moveTo(20f, 19f)
            horizontalLineTo(4f)
            verticalLineTo(17f)
            horizontalLineTo(20f)
            verticalLineTo(19f)
            close()
            moveTo(20f, 14f)
            horizontalLineTo(13f)
            verticalLineTo(8f)
            horizontalLineTo(20f)
            verticalLineTo(14f)
            close()
            moveTo(11f, 14f)
            horizontalLineTo(4f)
            verticalLineTo(8f)
            horizontalLineTo(11f)
            verticalLineTo(14f)
            close()
        }
    }

    val TrendingUp: ImageVector by lazy {
        buildIcon("TrendingUp") {
            moveTo(16f, 6f)
            lineTo(18.29f, 8.29f)
            lineTo(13.41f, 13.17f)
            lineTo(9.41f, 9.17f)
            lineTo(2f, 16.59f)
            lineTo(3.41f, 18f)
            lineTo(9.41f, 12f)
            lineTo(13.41f, 16f)
            lineTo(19.71f, 9.71f)
            lineTo(22f, 12f)
            verticalLineTo(6f)
            horizontalLineTo(16f)
            close()
        }
    }

    val Pets: ImageVector by lazy {
        buildIcon("Pets") {
            moveTo(12f, 11.5f)
            curveTo(9.51f, 11.5f, 7.5f, 13.51f, 7.5f, 16f)
            curveTo(7.5f, 18.49f, 9.51f, 20.5f, 12f, 20.5f)
            curveTo(14.49f, 20.5f, 16.5f, 18.49f, 16.5f, 16f)
            curveTo(16.5f, 13.51f, 14.49f, 11.5f, 12f, 11.5f)
            close()
            moveTo(4.5f, 10f)
            curveTo(5.6f, 10f, 6.5f, 8.88f, 6.5f, 7.5f)
            curveTo(6.5f, 6.12f, 5.6f, 5f, 4.5f, 5f)
            curveTo(3.4f, 5f, 2.5f, 6.12f, 2.5f, 7.5f)
            curveTo(2.5f, 8.88f, 3.4f, 10f, 4.5f, 10f)
            close()
            moveTo(8.5f, 7f)
            curveTo(9.6f, 7f, 10.5f, 5.88f, 10.5f, 4.5f)
            curveTo(10.5f, 3.12f, 9.6f, 2f, 8.5f, 2f)
            curveTo(7.4f, 2f, 6.5f, 3.12f, 6.5f, 4.5f)
            curveTo(6.5f, 5.88f, 7.4f, 7f, 8.5f, 7f)
            close()
            moveTo(15.5f, 7f)
            curveTo(16.6f, 7f, 17.5f, 5.88f, 17.5f, 4.5f)
            curveTo(17.5f, 3.12f, 16.6f, 2f, 15.5f, 2f)
            curveTo(14.4f, 2f, 13.5f, 3.12f, 13.5f, 4.5f)
            curveTo(13.5f, 5.88f, 14.4f, 7f, 15.5f, 7f)
            close()
            moveTo(19.5f, 10f)
            curveTo(20.6f, 10f, 21.5f, 8.88f, 21.5f, 7.5f)
            curveTo(21.5f, 6.12f, 20.6f, 5f, 19.5f, 5f)
            curveTo(18.4f, 5f, 17.5f, 6.12f, 17.5f, 7.5f)
            curveTo(17.5f, 8.88f, 18.4f, 10f, 19.5f, 10f)
            close()
        }
    }

    val FitnessCenter: ImageVector by lazy {
        buildIcon("FitnessCenter") {
            moveTo(20.57f, 14.86f)
            lineTo(22f, 13.43f)
            lineTo(20.57f, 12f)
            lineTo(17f, 15.57f)
            lineTo(8.43f, 7f)
            lineTo(12f, 3.43f)
            lineTo(10.57f, 2f)
            lineTo(9.14f, 3.43f)
            lineTo(7.71f, 2f)
            lineTo(5.57f, 4.14f)
            lineTo(4.14f, 2.71f)
            lineTo(2.71f, 4.14f)
            lineTo(4.14f, 5.57f)
            lineTo(2f, 7.71f)
            lineTo(3.43f, 9.14f)
            lineTo(2f, 10.57f)
            lineTo(3.43f, 12f)
            lineTo(7f, 8.43f)
            lineTo(15.57f, 17f)
            lineTo(12f, 20.57f)
            lineTo(13.43f, 22f)
            lineTo(14.86f, 20.57f)
            lineTo(16.29f, 22f)
            lineTo(18.43f, 19.86f)
            lineTo(19.86f, 21.29f)
            lineTo(21.29f, 19.86f)
            lineTo(19.86f, 18.43f)
            lineTo(22f, 16.29f)
            lineTo(20.57f, 14.86f)
            close()
        }
    }

    val Flight: ImageVector by lazy {
        buildIcon("Flight") {
            moveTo(21f, 16f)
            verticalLineTo(14f)
            lineTo(13f, 9f)
            verticalLineTo(3.5f)
            curveTo(13f, 2.67f, 12.33f, 2f, 11.5f, 2f)
            curveTo(10.67f, 2f, 10f, 2.67f, 10f, 3.5f)
            verticalLineTo(9f)
            lineTo(2f, 14f)
            verticalLineTo(16f)
            lineTo(10f, 13.5f)
            verticalLineTo(19f)
            lineTo(8f, 20.5f)
            verticalLineTo(22f)
            lineTo(11.5f, 21f)
            lineTo(15f, 22f)
            verticalLineTo(20.5f)
            lineTo(13f, 19f)
            verticalLineTo(13.5f)
            lineTo(21f, 16f)
            close()
        }
    }

    val CalendarToday: ImageVector by lazy {
        buildIcon("CalendarToday") {
            moveTo(20f, 3f)
            horizontalLineTo(19f)
            verticalLineTo(1f)
            horizontalLineTo(17f)
            verticalLineTo(3f)
            horizontalLineTo(7f)
            verticalLineTo(1f)
            horizontalLineTo(5f)
            verticalLineTo(3f)
            horizontalLineTo(4f)
            curveTo(2.9f, 3f, 2f, 3.9f, 2f, 5f)
            verticalLineTo(21f)
            curveTo(2f, 22.1f, 2.9f, 23f, 4f, 23f)
            horizontalLineTo(20f)
            curveTo(21.1f, 23f, 22f, 22.1f, 22f, 21f)
            verticalLineTo(5f)
            curveTo(22f, 3.9f, 21.1f, 3f, 20f, 3f)
            close()
            moveTo(20f, 21f)
            horizontalLineTo(4f)
            verticalLineTo(8f)
            horizontalLineTo(20f)
            verticalLineTo(21f)
            close()
        }
    }

    val Description: ImageVector by lazy {
        buildIcon("Description") {
            moveTo(14f, 2f)
            horizontalLineTo(6f)
            curveTo(4.9f, 2f, 4f, 2.9f, 4f, 4f)
            verticalLineTo(20f)
            curveTo(4f, 21.1f, 4.9f, 22f, 6f, 22f)
            horizontalLineTo(18f)
            curveTo(19.1f, 22f, 20f, 21.1f, 20f, 20f)
            verticalLineTo(8f)
            lineTo(14f, 2f)
            close()
            moveTo(16f, 18f)
            horizontalLineTo(8f)
            verticalLineTo(16f)
            horizontalLineTo(16f)
            verticalLineTo(18f)
            close()
            moveTo(16f, 14f)
            horizontalLineTo(8f)
            verticalLineTo(12f)
            horizontalLineTo(16f)
            verticalLineTo(14f)
            close()
            moveTo(13f, 9f)
            verticalLineTo(3.5f)
            lineTo(18.5f, 9f)
            horizontalLineTo(13f)
            close()
        }
    }

    val AutoAwesome: ImageVector by lazy {
        buildIcon("AutoAwesome") {
            moveTo(19f, 9f)
            lineTo(17.74f, 6.26f)
            lineTo(15f, 5f)
            lineTo(17.74f, 3.74f)
            lineTo(19f, 1f)
            lineTo(20.26f, 3.74f)
            lineTo(23f, 5f)
            lineTo(20.26f, 6.26f)
            close()
            moveTo(9f, 4f)
            lineTo(6.5f, 9.5f)
            lineTo(1f, 12f)
            lineTo(6.5f, 14.5f)
            lineTo(9f, 20f)
            lineTo(11.5f, 14.5f)
            lineTo(17f, 12f)
            lineTo(11.5f, 9.5f)
            close()
            moveTo(19f, 15f)
            lineTo(17.74f, 17.74f)
            lineTo(15f, 19f)
            lineTo(17.74f, 20.26f)
            lineTo(19f, 23f)
            lineTo(20.26f, 20.26f)
            lineTo(23f, 19f)
            lineTo(20.26f, 17.74f)
            close()
        }
    }

    val UploadFile: ImageVector by lazy {
        buildIcon("UploadFile") {
            moveTo(14f, 2f)
            horizontalLineTo(6f)
            curveTo(4.9f, 2f, 4f, 2.9f, 4f, 4f)
            verticalLineTo(20f)
            curveTo(4f, 21.1f, 4.9f, 22f, 6f, 22f)
            horizontalLineTo(18f)
            curveTo(19.1f, 22f, 20f, 21.1f, 20f, 20f)
            verticalLineTo(8f)
            lineTo(14f, 2f)
            close()
            moveTo(13f, 15f)
            verticalLineTo(18f)
            horizontalLineTo(11f)
            verticalLineTo(15f)
            horizontalLineTo(8f)
            lineTo(12f, 11f)
            lineTo(16f, 15f)
            horizontalLineTo(13f)
            close()
            moveTo(13f, 9f)
            verticalLineTo(3.5f)
            lineTo(18.5f, 9f)
            horizontalLineTo(13f)
            close()
        }
    }

    val Edit: ImageVector by lazy {
        buildIcon("Edit") {
            moveTo(3f, 17.25f)
            verticalLineTo(21f)
            horizontalLineTo(6.75f)
            lineTo(17.81f, 9.94f)
            lineTo(14.06f, 6.19f)
            lineTo(3f, 17.25f)
            close()
            moveTo(20.71f, 7.04f)
            curveTo(21.1f, 6.65f, 21.1f, 6.02f, 20.71f, 5.63f)
            lineTo(18.37f, 3.29f)
            curveTo(17.98f, 2.9f, 17.35f, 2.9f, 16.96f, 3.29f)
            lineTo(15.13f, 5.12f)
            lineTo(18.88f, 8.87f)
            lineTo(20.71f, 7.04f)
            close()
        }
    }

    val SwapHoriz: ImageVector by lazy {
        buildIcon("SwapHoriz") {
            moveTo(6.99f, 11f)
            lineTo(3f, 15f)
            lineTo(6.99f, 19f)
            verticalLineTo(16f)
            horizontalLineTo(14f)
            verticalLineTo(14f)
            horizontalLineTo(6.99f)
            verticalLineTo(11f)
            close()
            moveTo(21f, 9f)
            lineTo(17.01f, 5f)
            verticalLineTo(8f)
            horizontalLineTo(10f)
            verticalLineTo(10f)
            horizontalLineTo(17.01f)
            verticalLineTo(13f)
            lineTo(21f, 9f)
            close()
        }
    }

    val DarkMode: ImageVector by lazy {
        buildIcon("DarkMode") {
            moveTo(12f, 3f)
            curveTo(7.03f, 3f, 3f, 7.03f, 3f, 12f)
            curveTo(3f, 16.97f, 7.03f, 21f, 12f, 21f)
            curveTo(16.97f, 21f, 21f, 16.97f, 21f, 12f)
            curveTo(21f, 11.54f, 20.96f, 11.08f, 20.9f, 10.64f)
            curveTo(19.92f, 12.01f, 18.32f, 12.9f, 16.5f, 12.9f)
            curveTo(13.47f, 12.9f, 11f, 10.43f, 11f, 7.4f)
            curveTo(11f, 5.58f, 11.89f, 3.98f, 13.26f, 3.1f)
            curveTo(12.82f, 3.04f, 12.36f, 3f, 12f, 3f)
            close()
        }
    }

    val LightMode: ImageVector by lazy {
        buildIcon("LightMode") {
            moveTo(12f, 7f)
            curveTo(9.24f, 7f, 7f, 9.24f, 7f, 12f)
            curveTo(7f, 14.76f, 9.24f, 17f, 12f, 17f)
            curveTo(14.76f, 17f, 17f, 14.76f, 17f, 12f)
            curveTo(17f, 9.24f, 14.76f, 7f, 12f, 7f)
            close()
            moveTo(11f, 1f)
            horizontalLineTo(13f)
            verticalLineTo(4f)
            horizontalLineTo(11f)
            close()
            moveTo(11f, 20f)
            horizontalLineTo(13f)
            verticalLineTo(23f)
            horizontalLineTo(11f)
            close()
            moveTo(1f, 11f)
            horizontalLineTo(4f)
            verticalLineTo(13f)
            horizontalLineTo(1f)
            close()
            moveTo(20f, 11f)
            horizontalLineTo(23f)
            verticalLineTo(13f)
            horizontalLineTo(20f)
            close()
            moveTo(4.93f, 3.51f)
            lineTo(6.34f, 2.1f)
            lineTo(8.46f, 4.22f)
            lineTo(7.05f, 5.63f)
            close()
            moveTo(16.95f, 18.36f)
            lineTo(19.07f, 20.48f)
            lineTo(17.66f, 21.89f)
            lineTo(15.54f, 19.77f)
            close()
            moveTo(2.1f, 17.66f)
            lineTo(4.22f, 15.54f)
            lineTo(5.63f, 16.95f)
            lineTo(3.51f, 19.07f)
            close()
            moveTo(18.36f, 7.05f)
            lineTo(20.48f, 4.93f)
            lineTo(21.89f, 6.34f)
            lineTo(19.77f, 8.46f)
            close()
        }
    }

    val Palette: ImageVector by lazy {
        buildIcon("Palette") {
            moveTo(12f, 3f)
            curveTo(6.49f, 3f, 2f, 7.49f, 2f, 13f)
            curveTo(2f, 17.41f, 5.59f, 21f, 10f, 21f)
            curveTo(10.55f, 21f, 11f, 20.55f, 11f, 20f)
            curveTo(11f, 19.74f, 10.9f, 19.5f, 10.74f, 19.32f)
            curveTo(10.57f, 19.14f, 10.47f, 18.89f, 10.47f, 18.62f)
            curveTo(10.47f, 18.07f, 10.92f, 17.62f, 11.47f, 17.62f)
            horizontalLineTo(13f)
            curveTo(17.41f, 17.62f, 21f, 14.03f, 21f, 9.62f)
            curveTo(21f, 5.97f, 16.97f, 3f, 12f, 3f)
            close()
            moveTo(6.5f, 12f)
            curveTo(5.67f, 12f, 5f, 11.33f, 5f, 10.5f)
            reflectiveCurveTo(5.67f, 9f, 6.5f, 9f)
            reflectiveCurveTo(8f, 9.67f, 8f, 10.5f)
            reflectiveCurveTo(7.33f, 12f, 6.5f, 12f)
            close()
            moveTo(9.5f, 8f)
            curveTo(8.67f, 8f, 8f, 7.33f, 8f, 6.5f)
            reflectiveCurveTo(8.67f, 5f, 9.5f, 5f)
            reflectiveCurveTo(11f, 5.67f, 11f, 6.5f)
            reflectiveCurveTo(10.33f, 8f, 9.5f, 8f)
            close()
            moveTo(14.5f, 8f)
            curveTo(13.67f, 8f, 13f, 7.33f, 13f, 6.5f)
            reflectiveCurveTo(13.67f, 5f, 14.5f, 5f)
            reflectiveCurveTo(16f, 5.67f, 16f, 6.5f)
            reflectiveCurveTo(15.33f, 8f, 14.5f, 8f)
            close()
            moveTo(17.5f, 12f)
            curveTo(16.67f, 12f, 16f, 11.33f, 16f, 10.5f)
            reflectiveCurveTo(16.67f, 9f, 17.5f, 9f)
            reflectiveCurveTo(19f, 9.67f, 19f, 10.5f)
            reflectiveCurveTo(18.33f, 12f, 17.5f, 12f)
            close()
        }
    }

    val BrightnessAuto: ImageVector by lazy {
        buildIcon("BrightnessAuto") {
            moveTo(10.85f, 12.65f)
            horizontalLineTo(13.14f)
            lineTo(12.43f, 10.59f)
            lineTo(12f, 9.34f)
            lineTo(11.56f, 10.6f)
            lineTo(10.85f, 12.65f)
            close()
            moveTo(20f, 8.69f)
            verticalLineTo(4f)
            horizontalLineTo(15.31f)
            lineTo(12f, 0.69f)
            lineTo(8.69f, 4f)
            horizontalLineTo(4f)
            verticalLineTo(8.69f)
            lineTo(0.69f, 12f)
            lineTo(4f, 15.31f)
            verticalLineTo(20f)
            horizontalLineTo(8.69f)
            lineTo(12f, 23.31f)
            lineTo(15.31f, 20f)
            horizontalLineTo(20f)
            verticalLineTo(15.31f)
            lineTo(23.31f, 12f)
            lineTo(20f, 8.69f)
            close()
            moveTo(14.3f, 16f)
            lineTo(13.56f, 13.88f)
            horizontalLineTo(10.43f)
            lineTo(9.69f, 16f)
            horizontalLineTo(7.85f)
            lineTo(10.98f, 7.55f)
            horizontalLineTo(13.02f)
            lineTo(16.14f, 16f)
            horizontalLineTo(14.3f)
            close()
        }
    }
}


