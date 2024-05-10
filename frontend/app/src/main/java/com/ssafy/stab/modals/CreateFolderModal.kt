package com.ssafy.stab.modals

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.ssafy.stab.R
import com.ssafy.stab.apis.space.folder.createFolder
import com.ssafy.stab.data.PreferencesUtil
import com.ssafy.stab.screens.space.NoteListViewModel


@Composable
fun CreateFolderModal(closeModal: () -> Unit, viewModel: NoteListViewModel) {
    var folderName by remember { mutableStateOf("제목 없는 폴더") }
    val folderImg = painterResource(id = R.drawable.folder)


    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Image(painter = folderImg, contentDescription = null)
        Spacer(modifier = Modifier.height(20.dp))
        TextField(
            value = folderName,
            onValueChange = { folderName = it },
            label = { Text("폴더 이름") },
            modifier = Modifier
                .padding(10.dp)
                .fillMaxWidth(0.6f)
        )
        Row {
            Button(onClick = { closeModal() }) {
                Text(text = "취소")
            }
            Spacer(modifier = Modifier.width(30.dp))
            Button(onClick = { createFolder(
                PreferencesUtil.getNowLocation().nowLocation.toString(),
                folderName
            ){ response ->
                viewModel.addFolder(response)
            }
                closeModal()
            }) {
                Text(text = "생성")
            }
        }
    }
}