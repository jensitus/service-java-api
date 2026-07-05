package org.service.b.diary;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("diary")
public class DiaryController {

    @GetMapping
    public Diary getDiary() {
        Diary diary = new Diary();
        diary.setTitle("Himmel");
        diary.setText("Donner");
        return diary;
    }

}
