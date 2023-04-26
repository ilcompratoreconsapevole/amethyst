package com.vitorpamplona.amethyst.ui.actions

import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.util.Size
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CurrencyBitcoin
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.vitorpamplona.amethyst.R
import com.vitorpamplona.amethyst.model.Account
import com.vitorpamplona.amethyst.model.Note
import com.vitorpamplona.amethyst.service.model.TextNoteEvent
import com.vitorpamplona.amethyst.ui.components.*
import com.vitorpamplona.amethyst.ui.note.ReplyInformation
import com.vitorpamplona.amethyst.ui.screen.loggedIn.AccountViewModel
import com.vitorpamplona.amethyst.ui.screen.loggedIn.TextSpinner
import com.vitorpamplona.amethyst.ui.screen.loggedIn.UserLine
import com.vitorpamplona.amethyst.ui.theme.BitcoinOrange
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun NewPostView(onClose: () -> Unit, baseReplyTo: Note? = null, quote: Note? = null, account: Account, accountViewModel: AccountViewModel, navController: NavController) {
    val postViewModel: NewPostViewModel = viewModel()

    val context = LocalContext.current

    // initialize focus reference to be able to request focus programmatically
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    val scroolState = rememberScrollState()

    LaunchedEffect(Unit) {
        postViewModel.load(account, baseReplyTo, quote)
        delay(100)
        focusRequester.requestFocus()

        postViewModel.imageUploadingError.collect { error ->
            Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
        }
    }

    Dialog(
        onDismissRequest = { onClose() },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnClickOutside = false,
            decorFitsSystemWindows = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
            ) {
                Column(
                    modifier = Modifier
                        .padding(start = 10.dp, end = 10.dp, top = 10.dp)
                        .imePadding()
                        .weight(1f)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CloseButton(onCancel = {
                            postViewModel.cancel()
                            onClose()
                        })

                        PostButton(
                            onPost = {
                                postViewModel.sendPost()
                                onClose()
                            },
                            isActive = postViewModel.canPost()
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(scroolState)
                        ) {
                            if (postViewModel.replyTos != null && baseReplyTo?.event is TextNoteEvent) {
                                ReplyInformation(postViewModel.replyTos, postViewModel.mentions, account, "✖ ") {
                                    postViewModel.removeFromReplyList(it)
                                }
                            }

                            OutlinedTextField(
                                value = postViewModel.message,
                                onValueChange = {
                                    postViewModel.updateMessage(it)
                                },
                                keyboardOptions = KeyboardOptions.Default.copy(
                                    capitalization = KeyboardCapitalization.Sentences
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(
                                        width = 1.dp,
                                        color = MaterialTheme.colors.surface,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .focusRequester(focusRequester)
                                    .onFocusChanged {
                                        if (it.isFocused) {
                                            keyboardController?.show()
                                        }
                                    },
                                placeholder = {
                                    Text(
                                        text = stringResource(R.string.what_s_on_your_mind),
                                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.32f)
                                    )
                                },
                                colors = TextFieldDefaults
                                    .outlinedTextFieldColors(
                                        unfocusedBorderColor = Color.Transparent,
                                        focusedBorderColor = Color.Transparent
                                    ),
                                visualTransformation = UrlUserTagTransformation(MaterialTheme.colors.primary),
                                textStyle = LocalTextStyle.current.copy(textDirection = TextDirection.Content)
                            )

                            if (postViewModel.wantsPoll) {
                                postViewModel.pollOptions.values.forEachIndexed { index, _ ->
                                    NewPollOption(postViewModel, index)
                                }

                                Button(
                                    onClick = { postViewModel.pollOptions[postViewModel.pollOptions.size] = "" },
                                    border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.32f)),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = MaterialTheme.colors.onSurface.copy(alpha = 0.32f)
                                    )
                                ) {
                                    Image(
                                        painterResource(id = android.R.drawable.ic_input_add),
                                        contentDescription = "Add poll option button",
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            val url = postViewModel.contentToAddUrl
                            if (url != null) {
                                ImageVideoDescription(
                                    url,
                                    account.defaultFileServer,
                                    onAdd = { description, server ->
                                        postViewModel.upload(url, description, server, context)
                                        account.changeDefaultFileServer(server)
                                    },
                                    onCancel = {
                                        postViewModel.contentToAddUrl = null
                                    }
                                )
                            }

                            val user = postViewModel.account?.userProfile()
                            val lud16 = user?.info?.lnAddress()

                            if (lud16 != null && postViewModel.wantsInvoice) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 5.dp)) {
                                    InvoiceRequest(
                                        lud16,
                                        user.pubkeyHex,
                                        account,
                                        stringResource(id = R.string.lightning_invoice),
                                        stringResource(id = R.string.lightning_create_and_add_invoice),
                                        onSuccess = {
                                            postViewModel.message = TextFieldValue(postViewModel.message.text + "\n\n" + it)
                                            postViewModel.wantsInvoice = false
                                        },
                                        onClose = {
                                            postViewModel.wantsInvoice = false
                                        }
                                    )
                                }
                            }

                            val myUrlPreview = postViewModel.urlPreview
                            if (myUrlPreview != null) {
                                Row(modifier = Modifier.padding(top = 5.dp)) {
                                    if (isValidURL(myUrlPreview)) {
                                        val removedParamsFromUrl =
                                            myUrlPreview.split("?")[0].lowercase()
                                        if (imageExtensions.any { removedParamsFromUrl.endsWith(it) }) {
                                            AsyncImage(
                                                model = myUrlPreview,
                                                contentDescription = myUrlPreview,
                                                contentScale = ContentScale.FillWidth,
                                                modifier = Modifier
                                                    .padding(top = 4.dp)
                                                    .fillMaxWidth()
                                                    .clip(shape = RoundedCornerShape(15.dp))
                                                    .border(
                                                        1.dp,
                                                        MaterialTheme.colors.onSurface.copy(alpha = 0.12f),
                                                        RoundedCornerShape(15.dp)
                                                    )
                                            )
                                        } else if (videoExtensions.any { removedParamsFromUrl.endsWith(it) }) {
                                            VideoView(myUrlPreview)
                                        } else {
                                            UrlPreview(myUrlPreview, myUrlPreview)
                                        }
                                    } else if (isBechLink(myUrlPreview)) {
                                        BechLink(
                                            myUrlPreview,
                                            true,
                                            MaterialTheme.colors.background,
                                            accountViewModel,
                                            navController
                                        )
                                    } else if (noProtocolUrlValidator.matcher(myUrlPreview).matches()) {
                                        UrlPreview("https://$myUrlPreview", myUrlPreview)
                                    }
                                }
                            }
                        }
                    }

                    val userSuggestions = postViewModel.userSuggestions
                    if (userSuggestions.isNotEmpty()) {
                        LazyColumn(
                            contentPadding = PaddingValues(
                                top = 10.dp
                            ),
                            modifier = Modifier.heightIn(0.dp, 300.dp)
                        ) {
                            itemsIndexed(
                                userSuggestions,
                                key = { _, item -> item.pubkeyHex }
                            ) { _, item ->
                                UserLine(item, account) {
                                    postViewModel.autocompleteWithUser(item)
                                }
                            }
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth()) {
                        UploadFromGallery(
                            isUploading = postViewModel.isUploadingImage,
                            tint = MaterialTheme.colors.onBackground,
                            modifier = Modifier.padding(bottom = 10.dp)
                        ) {
                            postViewModel.selectImage(it)
                        }

                        if (postViewModel.canUsePoll) {
                            val hashtag = stringResource(R.string.poll_hashtag)
                            AddPollButton(postViewModel.wantsPoll) {
                                postViewModel.wantsPoll = !postViewModel.wantsPoll
                                postViewModel.includePollHashtagInMessage(postViewModel.wantsPoll, hashtag)
                            }
                        }

                        if (postViewModel.canAddInvoice) {
                            AddLnInvoiceButton(postViewModel.wantsInvoice) {
                                postViewModel.wantsInvoice = !postViewModel.wantsInvoice
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AddPollButton(
    isPollActive: Boolean,
    onClick: () -> Unit
) {
    IconButton(
        onClick = {
            onClick()
        }
    ) {
        if (!isPollActive) {
            Icon(
                painter = painterResource(R.drawable.ic_poll),
                null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colors.onBackground
            )
        } else {
            Icon(
                painter = painterResource(R.drawable.ic_lists),
                null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colors.onBackground
            )
        }
    }
}

@Composable
private fun AddLnInvoiceButton(
    isLnInvoiceActive: Boolean,
    onClick: () -> Unit
) {
    IconButton(
        onClick = {
            onClick()
        }
    ) {
        if (!isLnInvoiceActive) {
            Icon(
                imageVector = Icons.Default.CurrencyBitcoin,
                null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colors.onBackground
            )
        } else {
            Icon(
                imageVector = Icons.Default.CurrencyBitcoin,
                null,
                modifier = Modifier.size(20.dp),
                tint = BitcoinOrange
            )
        }
    }
}

@Composable
fun CloseButton(onCancel: () -> Unit) {
    Button(
        onClick = {
            onCancel()
        },
        shape = RoundedCornerShape(20.dp),
        colors = ButtonDefaults
            .buttonColors(
                backgroundColor = Color.Gray
            )
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_close),
            contentDescription = stringResource(id = R.string.cancel),
            modifier = Modifier.size(20.dp),
            tint = Color.White
        )
    }
}

@Composable
fun PostButton(onPost: () -> Unit = {}, isActive: Boolean, modifier: Modifier = Modifier) {
    Button(
        modifier = modifier,
        onClick = {
            if (isActive) {
                onPost()
            }
        },
        shape = RoundedCornerShape(20.dp),
        colors = ButtonDefaults
            .buttonColors(
                backgroundColor = if (isActive) MaterialTheme.colors.primary else Color.Gray
            )
    ) {
        Text(text = stringResource(R.string.post), color = Color.White)
    }
}

@Composable
fun SaveButton(onPost: () -> Unit = {}, isActive: Boolean, modifier: Modifier = Modifier) {
    Button(
        modifier = modifier,
        onClick = {
            if (isActive) {
                onPost()
            }
        },
        shape = RoundedCornerShape(20.dp),
        colors = ButtonDefaults
            .buttonColors(
                backgroundColor = if (isActive) MaterialTheme.colors.primary else Color.Gray
            )
    ) {
        Text(text = stringResource(R.string.save), color = Color.White)
    }
}

@Composable
fun CreateButton(onPost: () -> Unit = {}, isActive: Boolean, modifier: Modifier = Modifier) {
    Button(
        modifier = modifier,
        onClick = {
            if (isActive) {
                onPost()
            }
        },
        shape = RoundedCornerShape(20.dp),
        colors = ButtonDefaults
            .buttonColors(
                backgroundColor = if (isActive) MaterialTheme.colors.primary else Color.Gray
            )
    ) {
        Text(text = stringResource(R.string.create), color = Color.White)
    }
}

@Composable
fun SearchButton(onPost: () -> Unit = {}, isActive: Boolean, modifier: Modifier = Modifier) {
    Button(
        modifier = modifier,
        onClick = {
            if (isActive) {
                onPost()
            }
        },
        shape = RoundedCornerShape(20.dp),
        colors = ButtonDefaults
            .buttonColors(
                backgroundColor = if (isActive) MaterialTheme.colors.primary else Color.Gray
            )
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_search),
            null,
            modifier = Modifier.size(26.dp),
            tint = Color.White
        )
    }
}

enum class ServersAvailable() {
    IMGUR
}

@Composable
fun ImageVideoDescription(
    uri: Uri,
    defaultServer: ServersAvailable,
    onAdd: (String, ServersAvailable) -> Unit,
    onCancel: () -> Unit
) {
    val resolver = LocalContext.current.contentResolver
    val mediaType = resolver.getType(uri) ?: ""
    val scope = rememberCoroutineScope()

    val isImage = mediaType.startsWith("image")
    val isVideo = mediaType.startsWith("video")

    val fileServers = listOf(
        Pair(ServersAvailable.IMGUR, "imgur.com")
    )

    val fileServerOptions = fileServers.map { it.second }

    var selectedServer by remember { mutableStateOf(defaultServer) }
    var message by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 30.dp, end = 30.dp)
            .clip(shape = RoundedCornerShape(10.dp))
            .border(
                1.dp,
                MaterialTheme.colors.onSurface.copy(alpha = 0.12f),
                RoundedCornerShape(15.dp)
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(30.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp)
            ) {
                Text(
                    text = stringResource(
                        if (isImage) {
                            R.string.content_description_add_image
                        } else {
                            if (isVideo) {
                                R.string.content_description_add_video
                            } else {
                                R.string.content_description_add_document
                            }
                        }
                    ),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.W500,
                    modifier = Modifier
                        .padding(start = 10.dp)
                        .weight(1.0f)
                )

                IconButton(
                    modifier = Modifier.size(30.dp),
                    onClick = onCancel
                ) {
                    Icon(
                        imageVector = Icons.Default.Cancel,
                        null,
                        modifier = Modifier
                            .padding(end = 5.dp)
                            .size(30.dp),
                        tint = MaterialTheme.colors.onSurface.copy(alpha = 0.32f)
                    )
                }
            }

            Divider()

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp)
            ) {
                if (mediaType.startsWith("image")) {
                    AsyncImage(
                        model = uri.toString(),
                        contentDescription = uri.toString(),
                        contentScale = ContentScale.FillWidth,
                        modifier = Modifier
                            .padding(top = 4.dp)
                            .fillMaxWidth()
                    )
                } else if (mediaType.startsWith("video") && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    var bitmap by remember { mutableStateOf<Bitmap?>(null) }

                    LaunchedEffect(key1 = uri) {
                        scope.launch(Dispatchers.IO) {
                            bitmap = resolver.loadThumbnail(uri, Size(1200, 1000), null)
                        }
                    }

                    bitmap?.let {
                        Image(
                            bitmap = it.asImageBitmap(),
                            contentDescription = "some useful description",
                            contentScale = ContentScale.FillWidth,
                            modifier = Modifier
                                .padding(top = 4.dp)
                                .fillMaxWidth()
                        )
                    }
                } else {
                    VideoView(uri)
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                TextSpinner(
                    label = stringResource(id = R.string.file_server),
                    placeholder = fileServers.filter { it.first == defaultServer }.first().second,
                    options = fileServerOptions,
                    onSelect = {
                        selectedServer = fileServers[it].first
                    },
                    modifier = Modifier
                        .weight(1f)
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    label = { Text(text = stringResource(R.string.content_description)) },
                    modifier = Modifier.fillMaxWidth(),
                    value = message,
                    onValueChange = { message = it },
                    placeholder = {
                        Text(
                            text = stringResource(R.string.content_description_example),
                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.32f)
                        )
                    },
                    keyboardOptions = KeyboardOptions.Default.copy(
                        capitalization = KeyboardCapitalization.Sentences
                    )
                )
            }

            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                onClick = {
                    onAdd(message, selectedServer)
                },
                shape = RoundedCornerShape(15.dp),
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = MaterialTheme.colors.primary
                )
            ) {
                Text(text = stringResource(R.string.add_content), color = Color.White, fontSize = 20.sp)
            }
        }
    }
}
