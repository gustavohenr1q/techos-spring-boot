
package br.com.bugnacio.techos.service;

import br.com.bugnacio.techos.model.TipoFoto;
import br.com.bugnacio.techos.repository.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FotosEquipamentoServiceTest {

    @Mock
    EquipamentoRepository equipamentoRepo;

    @Mock
    RegistroFotograficoRepository registroRepo;

    @Mock
    FotoEquipamentoRepository fotoRepo;

    @Mock
    FotoStorageService storage;

    @InjectMocks
    FotosEquipamentoService service;

    @Test
    void naoDeveEnviarFotoParaRegistroDeOutroEquipamento() {

        when(registroRepo.findByIdAndEquipamento_Id(10L, 7L))
                .thenReturn(Optional.empty());

        assertThrows(
                IllegalArgumentException.class,
                () -> service.enviar(
                        7L,
                        10L,
                        TipoFoto.ENTRADA,
                        new MultipartFile[0]
                )
        );

        verifyNoInteractions(storage);
        verify(fotoRepo, never()).save(any());
    }
}
